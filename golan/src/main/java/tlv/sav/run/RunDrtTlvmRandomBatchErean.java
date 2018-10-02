package tlv.sav.run;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

//TODO change config to match scores of original TLVM
//TODO run with 5000 cars flow storage fixed
//TODO run with 10000 cars flow storage fixed
//TODO run with 20000 cars flow storage fixed
//TODO remove last iteration to 2
//TODO remove write events intercals

public class RunDrtTlvmRandomBatchErean {
	final static String CONFIG_FILE = "tlvmdrtconfig.xml";
	final static String OUTPUT_FOLDER = "src\\main\\resources\\drt_tlvm\\output\\";
	final static String INPUT_FOLDER = "src\\main\\resources\\drt_tlvm\\input\\";
	
	
//TODO POP FILE 2 CHANGE OUTPUT FOLDER FILL RUN CHANGE ALPHA BETA change iters to 15
	
	
	public static void main(String[] args) {
		int popFile = 2; // popfile = 1 (tlvm_sav_input based), 2 (sample_tlvm_sav_cadyst_wo_routes), 3
							// (tlvm_sav_cadyst_wo_routes)
		int[] savVehiclesArr = new int[] { 5000, 10000, 15000, 20000, 30000, 40000 };
		int[] savVehicleSeatssArr = new int[] { 1, 2, 3, 4};
		int runId = 14;
		for (int i = 0; i < savVehiclesArr.length; i++) {
			int savVehicles = savVehiclesArr[i];
			for (int j = 0; j < savVehicleSeatssArr.length; j++) {
				int savSeats = savVehicleSeatssArr[j];
				run(false, popFile,savVehicles,savSeats,Integer.toString(runId)); // switch to 'true' to turn on visualisation
				runId++;
			}
			
		}

	}

	public static void run(boolean otfvis, int popFile, int savVehicles , int savSeats, String runId) {
//		String runId = "14";
		int sampleSize = 100; // sample size pop, only if popFile = 2
		int cores = Runtime.getRuntime().availableProcessors(); // get comp cores
		int iters = 0;
		double alpha= 1.5;
		double beta = 870;
		double maxWait = 720;
//		double flowCapacity = 0.11;
//		double storagExpo = 0.75;
//		double storageCapacity = Math.pow(flowCapacity, storagExpo);

		Config config = ConfigUtils.loadConfig(INPUT_FOLDER + CONFIG_FILE, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		// defualt MATSim config
		config.network().setInputFile("Network_9_model_4_walk_to_train_only_clean_modal_fix_capac.xml");
//		config.global().setNumberOfThreads(15);
//		config.qsim().setNumberOfThreads(15);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setRunId(runId);
		config.controler().setOutputDirectory(
				OUTPUT_FOLDER+"test_alpha_"+alpha+"_beta_"+((int)beta)+"_max_"+((int)maxWait)+"\\"+ runId + "_tlvm_random_distributed_"+savSeats+"_seater_" + savVehicles + "_sav_taxis_no_tta_iters_"+iters);
//		config.controler().setLastIteration(6);
//		config.controler().setWritePlansInterval(1);
		config.controler().setWriteEventsInterval(5);
		config.controler().setLastIteration(iters);

		if (popFile == 1)
			config.plans().setInputFile("tlvm_sav_drt_population_input_based.xml");
		else if (popFile == 2)
			config.plans().setInputFile(
					"test_populations\\" + sampleSize + "_tlvm_sav_drt_population_cadyst_2013_wo_routs.xml");
		else if (popFile == 3)
			config.plans().setInputFile("tlvm_sav_drt_population_cadyst_2013_wo_routs_wo_tta.xml");
		config.facilities().setInputFile("facilities.xml");
//		config.qsim().setFlowCapFactor(flowCapacity);
//		config.qsim().setStorageCapFactor(storageCapacity);

		DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);
//		drt.setVehiclesFile("vehicles\\tlvm_ototel_sav_taxis.xml");
		drt.setVehiclesFile("vehicles\\tlvm_random_distributed_"+savSeats+"_seater_" + savVehicles + "_sav_taxis.xml");
		drt.setPrintDetailedWarnings(false); // remove print of errors
//		drt.setIdleVehiclesReturnToDepots(true);
//		drt.setOperationalScheme(operationalScheme);
//		drt.setPlotDetailedCustomerStats(true);
		drt.setNumberOfThreads(cores);
		drt.setPlotDetailedCustomerStats(true);
		drt.setIdleVehiclesReturnToDepots(false);
		drt.setMaxTravelTimeAlpha(alpha);
		drt.setMaxTravelTimeBeta(beta);
		drt.setMaxWaitTime(maxWait);

		Controler controler = DrtControlerCreator.createControler(config, false);
		controler.run();

	}

}
