
import org.junit.Test;
import org.pitest.functional.predicate.Predicate;
import org.pitest.mutationtest.config.PluginServices;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.tooling.AnalysisResult;
import org.pitest.mutationtest.tooling.EntryPoint;
import org.pitest.testapi.TestGroupConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 26/07/17
 */

public class PitRunnerTest {

	@Test
	public void testPitRunnerOriginal() throws Exception {


		long time = System.currentTimeMillis();

		final ReportOptions options = new ReportOptions();

		List<File> sources = new ArrayList<>();
		sources.add(new File("src/main/java"));
		sources.add(new File("src/test/java"));

		final PluginServices plugins = PluginServices.makeForContextLoader();

		options.setReportDir("out");
		options.setTargetClasses(Collections.singletonList(new Predicate<String>() {
			@Override
			public Boolean apply(String s) {
				return "example.Example".equals(s);
			}
		}));
		options.setTargetTests(Collections.singletonList(new Predicate<String>() {
			@Override
			public Boolean apply(String s) {
				return "example.TestSuiteExample".equals(s);
			}
		}));
		options.setSourceDirs(sources);
		options.setGroupConfig(new TestGroupConfig());

		final EntryPoint entry = new EntryPoint();
		final AnalysisResult result = entry.execute(
				null,
				options, plugins, Collections.emptyMap()
		);

		System.out.println(System.currentTimeMillis() - time);
	}
}
