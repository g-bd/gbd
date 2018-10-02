package tlv.sav.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


//TODO change config to match scores of original TLVM
//TODO run with 5000 cars flow storage fixed
//TODO run with 10000 cars flow storage fixed
//TODO run with 20000 cars flow storage fixed
//TODO remove last iteration to 2
//TODO remove write events intercals

public class RunDrtTlvmRandomCountCompare
{
//	final static String CONFIG_FILE = "tlvmdrtconfigsimple.xml";
	final static String CONFIG_FILE = "25.output_config.xml";
	final static String OUTPUT_FOLDER = "src\\main\\resources\\drt_tlvm\\drt_paper_draft\\analysis\\batch_alpha_1.5_beta_900_max_720\\rejections on\\25_tlvm_random_distributed_4_seater_15000_sav_taxis_no_tta_iters_15\\sav_counts_compare";
	final static String INPUT_FOLDER =  "src\\main\\resources\\drt_tlvm\\drt_paper_draft\\analysis\\batch_alpha_1.5_beta_900_max_720\\rejections on\\25_tlvm_random_distributed_4_seater_15000_sav_taxis_no_tta_iters_15\\";
	
	public static void main(String[] args) {
		int popFile= 3;			// popfile = 1 (tlvm_sav_input based), 2 (sample_tlvm_sav_cadyst_wo_routes), 3 (tlvm_sav_cadyst_wo_routes)
		run(false,popFile); // switch to 'true' to turn on visualisation
	}
	public static void run(boolean otfvis, int popFile ) {
		int sampleSize = 2000; // sample size pop, only if popFile = 2
		int savVehicles = 20000;
		int cores = Runtime.getRuntime().availableProcessors(); 	//get  comp cores
		int iters = 2;
//		double flowCapacity = 0.11;
//		double storagExpo = 0.75;
//		double storageCapacity = Math.pow(flowCapacity, storagExpo);
		
		Config config = ConfigUtils.loadConfig(INPUT_FOLDER+CONFIG_FILE, new DrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());

		//defualt MATSim config
		config.network().setInputFile("D:\\git\\golan\\src\\main\\resources\\tlvm_calibrated_raptor\\input\\Network_9_model_4_walk_to_train_only_clean_modal_fix_capac.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(iters);

		config.controler().setWriteEventsInterval(1);

		
		config.counts().setInputFile("D:\\git\\golan\\src\\main\\resources\\tlvm_calibrated_raptor\\input\\counts_2013_no_outliers.xml");
		config.counts().setCountsScaleFactor(10);
		config.counts().setWriteCountsInterval(1);

		
		if(popFile == 1)
		config.plans().setInputFile("tlvm_sav_drt_population_input_based.xml");
		else if(popFile == 2 ) {
		config.plans().setInputFile("test_populations\\"+sampleSize+"_tlvm_sav_drt_population_cadyst_2013_wo_routs.xml");
		config.controler().setOutputDirectory(OUTPUT_FOLDER+"_samplesize_"+sampleSize+"_tlvm_random_distributed_4_seater_"+savVehicles+"_sav_taxis");
		}
		else if(popFile ==3)
		{
		config.plans().setInputFile("25.output_plans.xml.gz");
		config.controler().setOutputDirectory(OUTPUT_FOLDER);

		}
		config.facilities().setInputFile("25.output_facilities.xml.gz");
//		config.qsim().setFlowCapFactor(flowCapacity);
//		config.qsim().setStorageCapFactor(storageCapacity);

		
		DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
//		drt.setVehiclesFile("vehicles\\tlvm_ototel_sav_taxis.xml");
		drt.setVehiclesFile("D:\\git\\golan\\src\\main\\resources\\drt_tlvm\\input\\vehicles\\int\\tlvm_random_distributed_4_seater_"+savVehicles+"_sav_taxis.xml");
		drt.setPrintDetailedWarnings(false);		// remove print of errors
//		drt.setIdleVehiclesReturnToDepots(true);
//		drt.setOperationalScheme(operationalScheme);
//		drt.setPlotDetailedCustomerStats(true);
		drt.setNumberOfThreads(cores);
//		drt.setPlotDetailedVehicleStats(true);
		drt.setIdleVehiclesReturnToDepots(false);
		//set alpha based on paper
		drt.setMaxTravelTimeAlpha(1.5);
		drt.setMaxWaitTime(900);
		drt.setMaxTravelTimeBeta(720);
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);

		
		Controler controler = DrtControlerCreator.createControler(config, false);
		// make the controler react to facility open times:
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				CharyparNagelOpenTimesScoringFunctionFactory factory = new CharyparNagelOpenTimesScoringFunctionFactory(
						scenario);
				this.bindScoringFunctionFactory().toInstance(factory);
			}
		});
		controler.run();
		
		
	}


}
