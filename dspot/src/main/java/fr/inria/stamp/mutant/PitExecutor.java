package fr.inria.stamp.mutant;

import fr.inria.diversify.automaticbuilder.AutomaticBuilderFactory;
import fr.inria.diversify.utils.sosiefier.InputConfiguration;
import fr.inria.stamp.Main;
import org.pitest.mutationtest.config.PluginServices;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.config.SettingsFactory;
import org.pitest.mutationtest.tooling.EntryPoint;
import org.pitest.testapi.TestGroupConfig;
import org.pitest.util.Glob;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 15/12/17
 */
// this class use a custom classloader, which is wrong since we want to isolate executions
public class PitExecutor {

    public static void execute(InputConfiguration configuration, String fullQualifiedNameOfTestClass) {
        final String programDir = configuration.getInputProgram().getProgramDir();
        final ReportOptions data = createReportOptions(configuration, programDir, fullQualifiedNameOfTestClass);
        final SettingsFactory settingsFactory = createSettingFactory(configuration, data, programDir);
        final EntryPoint e = new EntryPoint();
        e.execute(new File(programDir), data, settingsFactory, Collections.emptyMap());
    }

    private static ReportOptions createReportOptions(InputConfiguration configuration, String programDir, String fullQualifiedNameOfTestClass) {
        final ReportOptions data = new ReportOptions();
        final List<String> classpathList = Arrays.stream(((URLClassLoader) ClassLoader.getSystemClassLoader())
                .getURLs())
                .filter(url -> url.toString().contains("pit"))
                .map(URL::getFile)
                .collect(Collectors.toList());
        classpathList.add(programDir + configuration.getInputProgram().getClassesDir());
        classpathList.add(programDir + configuration.getInputProgram().getTestClassesDir());
        classpathList.addAll(Arrays.asList(AutomaticBuilderFactory.getAutomaticBuilder(configuration).buildClasspath(programDir).split(":")));
        data.setClassPathElements(classpathList);
        data.setDependencyAnalysisMaxDistance(-1);
        data.setTargetClasses(Glob.toGlobPredicates(Arrays.asList(configuration.getProperty("filter"))));
        if (fullQualifiedNameOfTestClass != null) {
            data.setTargetTests(Glob.toGlobPredicates(Arrays.asList(fullQualifiedNameOfTestClass)));
        }
        data.setReportDir(programDir + "target/report-pits/");
        data.setVerbose(Main.verbose);
        data.setMutators(Collections.singletonList("ALL"));
        data.setSourceDirs(Collections.singletonList(new File(programDir)));
        data.addOutputFormats(Collections.singletonList("CSV"));
        final TestGroupConfig testGroupConfig = new TestGroupConfig(Collections.emptyList(), Collections.emptyList());
        data.setGroupConfig(testGroupConfig);
        data.setExportLineCoverage(true);
        data.setMutationEngine("gregor");
        return data;
    }

    private static SettingsFactory createSettingFactory(InputConfiguration configuration, ReportOptions data, String programDir) {
        final String classpath = AutomaticBuilderFactory.getAutomaticBuilder(configuration).buildClasspath(programDir);
        final URL[] urls = Arrays.stream(classpath.split(":"))
                .map(File::new)
                .map(file -> {
                    try {
                        return file.toURI().toURL();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).toArray(URL[]::new);
        ClassLoader classLoader = new URLClassLoader(urls);
        return new SettingsFactory(data, new PluginServices(classLoader));
    }


}
