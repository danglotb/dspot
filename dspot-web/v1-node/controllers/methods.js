// Mongodb
const MONGODB_HOST = process.env.MONGODB_HOST || "mongodb://localhost:27017";
const dbName = "Dspot" || process.env.MONGODB_NAME;
const colName = "AmpRecords" || process.env.MONGODB_COLNAME;
const MongoClient = require('mongodb').MongoClient;
// ActiveMQ 
const stompit = require('stompit');
const activemq_queuename = process.env.ACTIVEMQ_QUEUENAME || "Dpipeline";;

// Others
const assert = require('assert');
const async = require("async");

/**
 * Extract the request url path and fetch the corresponding page and 
 * send it to the user
 */
function get_page(req, res, next) {
    var path = ''
    if (req.originalUrl == '/') {
        path = 'pages/index.html'
    } else {
        path = 'pages' + req.originalUrl + '.html'
    }
    res.sendFile(path, { root: __dirname });
}

function fetchData(colName, query, limit, res) {
    // Database Name
    /* query = {};
    query["repoName"] = reqslug; */
    if (query == undefined) {
        query = {};
    }
    const dbName = "Dspot" //process.env.MONGODB_NAME;
    MongoClient.connect(MONGODB_HOST, { useNewUrlParser: true }, function(err, client) {
        assert.equal(null, err);
        const db = client.db(dbName);
        console.log("Connected to mongo");

        if (limit != undefined) {
            db.collection(colName).find(query,{projection:{_id: 0,"Email":0}}).sort({ Date: -1 }).limit(limit).toArray(function(err, result) {
                if (err) {
                    res.json(err);
                } else {
                    /*console.log(result)*/
                    res.json(result);
                }
            });
        } else {
            db.collection(colName).find(query,{projection:{_id: 0,"Email":0}}).sort({ Date: -1 }).toArray(function(err, result) {
                if (err) {
                    console.log(err);
                    res.json(err);
                } else {
                    /*console.log(result);*/
                    res.json(result);
                }
            });
        }
        client.close();
    });
}

/**
 * Send message to ActiveMQ queue
 * @param message is a string
 */
function sendMessageToActiveMQ(message) {
    return Promise.resolve(stompit.connect({ host: process.env.ACTIVEMQ_HOSTNAME || 'localhost', port: 61613}, 
        (err, client) => {
        var queueName = activemq_queuename;
        const frame = client.send({ destination: queueName});
        frame.write(message);
        frame.end();
        console.log("Message sended to " + queueName)
        client.disconnect();
    }));
}

/**
 * This fetch the template page for any repository
 * Data will be sent over later for the specific repo
 */
exports.get_reposTemplatePage = function(req, res, next) {
    console.log("Getting repos template page");
    res.sendFile('pages/reposTemplate.html', { root: __dirname });
}

/**
 * Fetch data, give slug and branch, return result for both
 * Pitmutant- and JacocoSelector
 */
exports.get_repoInfoData = function(req, res, next) {
    var query = {};
    query["RepoSlug"] = req.params.user + "/" + req.params.reponame;
    query["RepoBranch"] = req.params.branchname;
    extra = {$or:[{"State":{$eq:"old"}},{"State":{$eq:"recent"}}]};
    finalQ = {$and:[query,extra]};
    fetchData('AmpRecords', finalQ, undefined, res);
}

/**
 * For the start page fetch data for displaying 3 most 
 * recent scanned repos.
 */
exports.get_ReposData = function(req, res, next) {
    console.log("getting most 3 recent repos data");
    /* res.sendFile( "." + req.originalUrl,{root: __dirname }); */
    /* Check if the requested data is which selector otherwise take 3 most*/
    if (req.params.state == "recent") {
        fetchData('AmpRecords', {"State": "recent"}, 3, res);
    } else if (req.params.state == "All") { /*Only take recent and pending state*/
        fetchData('AmpRecords',{$or:[{"State":{$eq:"recent"}},{"State":{$eq:"pending"}}]},undefined,res);
    }
}

/**
 * When someone submit at the home page this function
 * will check if there is already a unhandled request
 * previously by checking if there is a pending state 
 * document in the database. If not it will submit a 
 * new doc with pending state with the submitted information
 */
function post_submitRepo(req, res, next) {
    const url = require('url');

    if (!validateEmail(req.body.email)) {
        return Promise.resolve(res.status(400).send("Invalid email"));
    }  

    let repoSlug = url.parse(req.body.repo.url).pathname.substring(1);
    /*let repoBranch = req.body.repo.branch;*/
    let repoBranch = "master" /*Always master - will be removed in the future*/
    let selector = req.body.dspot.selector;
    /*Constructing query*/
    var query = {};
    query["RepoSlug"] = repoSlug;
    query["RepoBranch"] = repoBranch;
    query["State"] = "pending";
    query["Email"] = req.body.email;


    var ampOptions = {};
    ampOptions['test-criterion'] = selector;
    query["AmpOptions"] = ampOptions;

    MongoClient.connect(MONGODB_HOST, { useNewUrlParser: true }, function(err, client) {
        assert.equal(null, err);
        const db = client.db(dbName);
        console.log("Connected to mongo");

        // This fetch the secret associated with the slug and check it
        db.collection(colName).findOne(query, async function(err, result) {
            if (err) {
                console.log(err);
            } else {
                if (result == null) {
                    console.log("Proceed initializing a pending document");

                    var datetime = new Date().toISOString().split('.')[0]+"Z";

                    query["Date"] = datetime;
                    await db.collection(colName).insertOne(query, function(err, res) {
                        if (err) throw err;
                        console.log("1 document inserted");
                    });
                    sendMessageToActiveMQ(repoSlug + "," + repoBranch + "," + selector)
                    res.status(200).send("Success!\n We'll comeback to you over email. The Dspot team. \n\n (you can now safely close this window)");
                } else {
                    /*console.log(result);*/
                    res.status(400).send("Already existed a similar pending request");
                }
                client.close();
            }
        });
    });
}

function validateEmail(email) {
    var re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
    return re.test(String(email).toLowerCase());
}


exports.get_page = get_page;
exports.post_submitRepo = post_submitRepo;