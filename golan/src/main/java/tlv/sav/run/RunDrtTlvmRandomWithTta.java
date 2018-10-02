package tlv.sav.run;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

//TODO change config to match scores of original TLVM
//TODO run with 5000 cars flow storage fixed
//TODO run with 10000 cars flow storage fixed
//TODO run with 20000 cars flow storage fixed
//TODO remove last iteration to 2
//TODO remove write events intercals

/**
 * @author Golan-New_PC
 *
 */
public class RunDrtTlvmRandomWithTta
{
	
	final static String CONFIG_FILE = "‏‏tlvmdrtconfignoreplanning.xml";
//	final static String CONFIG_FILE = "‏‏tlvmdrtconfig.xml";
	final static String OUTPUT_FOLDER = "src\\main\\resources\\drt_tlvm\\output\\";
//	final static String OUTPUT_FOLDER = "src\\main\\resources\\drt_tlvm\\output\\test";
	final static String INPUT_FOLDER = "src\\main\\resources\\drt_tlvm\\input\\";

	public static void main(String[] args)
	{
		int popFile = 3; // popfile = 1 (tlvm_sav_input based), 2 (sample_tlvm_sav_cadyst_wo_routes), 3
		// (tlvm_sav_cadyst_wo_routes)
		int savVehicles = 15000;
		int savSeats = 4;
		int runId = 63;
		boolean isEnableRequestRejection = false;
		boolean isTta = true;
		run(false, popFile, savVehicles, savSeats, Integer.toString(runId), isEnableRequestRejection, isTta);

	}

	/**
	 * @param otfvis
	 *            - visual
	 * @param popFile
	 *            - samplesize (int)
	 * @param savVehicles
	 *            - fleet size (5000-4000) int
	 * @param savSeats
	 *            - seats - 1-4 -int
	 * @param runId
	 * @param isEnableRequestRejection
	 * @param isTta
	 *            - external traffic
	 */
	public static void run(boolean otfvis, int popFile, int savVehicles, int savSeats, String runId,
			boolean isEnableRequestRejection, boolean isTta)
	{
		int sampleSize = 2000; // sample size pop, only if popFile = 2
		int cores = Runtime.getRuntime().availableProcessors(); // get comp cores
		int iters = 15;
		double alpha = 1.5;
		double beta = 900;
		double maxWait = 720;
		// double flowCapacity = 0.11;
		// double storagExpo = 0.75;
		// double storageCapacity = Math.pow(flowCapacity, storagExpo);

		Config config = ConfigUtils.loadConfig(INPUT_FOLDER + CONFIG_FILE, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());

		// defualt MATSim config
		config.network().setInputFile("Network_9_model_4_walk_to_train_only_clean_modal_fix_capac.xml");
		// config.global().setNumberOfThreads(15);
		// config.qsim().setNumberOfThreads(15);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setRunId(runId);
		config.controler()
				.setOutputDirectory(OUTPUT_FOLDER + alpha + "_beta_" + ((int) beta) + "_max_" + ((int) maxWait)
						+ "_request_rejection_" + String.valueOf(isEnableRequestRejection) + "\\" + runId
						+ "_tlvm_random_distributed_" + savSeats + "_seater_" + savVehicles + "_sav_taxis_tta_"
						+ String.valueOf(isTta) + "_iters_" + iters);
		// config.controler().setLastIteration(6);
		// config.controler().setWritePlansInterval(1);
		config.controler().setWriteEventsInterval(5);
		config.controler().setLastIteration(iters);

		if (popFile == 1)
			config.plans().setInputFile("tlvm_sav_drt_population_input_based.xml");
		else if (popFile == 2)
			config.plans().setInputFile(
					"test_populations\\" + sampleSize + "_tlvm_sav_drt_population_cadyst_2013_wo_routs.xml");
		else if (popFile == 3)
		{
			if (isTta)
			{
				config.plans().setInputFile("tlvm_sav_drt_population_cadyst_2013_wo_routs_with_tta.xml");
				config.plans().setSubpopulationAttributeName("subpopulation");
				config.plans().setInputPersonAttributeFile("personAtrributes-with-subpopulation.xml");
				config.strategy().setMaxAgentPlanMemorySize(5);
				// innovation will be stop after iter * fraction . eg if 100 iters then at 80
				config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
				//plan selector
				StrategyConfigGroup.StrategySettings changeExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
				changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
				changeExpBetaStrategySettings.setWeight(0.7); 
				config.strategy().addStrategySettings(changeExpBetaStrategySettings);
				// time-mutation internal agents
				StrategySettings timeMutatorInternal = new StrategySettings();
				timeMutatorInternal.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
				timeMutatorInternal.setWeight(0.1);
				timeMutatorInternal.setDisableAfter(-1);
				timeMutatorInternal.setSubpopulation("internalAgent");
				config.strategy().addStrategySettings(timeMutatorInternal);
				
			}
			else
			{
				config.plans().setInputFile("tlvm_sav_drt_population_cadyst_2013_wo_routs_wo_tta.xml");
				config.strategy().setMaxAgentPlanMemorySize(5);
				// innovation will be stop after iter * fraction . eg if 100 iters then at 80
				config.strategy().setFractionOfIterationsToDisableInnovation(0.8);
				//plan selector
				StrategyConfigGroup.StrategySettings changeExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
				changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
				changeExpBetaStrategySettings.setWeight(0.7);
				config.strategy().addStrategySettings(changeExpBetaStrategySettings);
				// time-mutation internal agents
				StrategySettings timeMutatorInternal = new StrategySettings();
				timeMutatorInternal.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
				timeMutatorInternal.setWeight(0.1);
				config.strategy().addStrategySettings(timeMutatorInternal);
				

			}	
		}
		config.facilities().setInputFile("facilities.xml");
		// config.qsim().setFlowCapFactor(flowCapacity);
		// config.qsim().setStorageCapFactor(storageCapacity);

		DrtConfigGroup drt = (DrtConfigGroup) config.getModules().get(DrtConfigGroup.GROUP_NAME);

		// drt.setVehiclesFile("vehicles\\tlvm_ototel_sav_taxis.xml");
		drt.setVehiclesFile("vehicles\\tlvm_random_distributed_" + savSeats + "_seater_" + savVehicles + "_sav_taxis.xml");
		drt.setPrintDetailedWarnings(false); // remove print of errors
		// drt.setIdleVehiclesReturnToDepots(true);
		// drt.setOperationalScheme(operationalScheme);
		// drt.setPlotDetailedCustomerStats(true);
		drt.setNumberOfThreads(cores);
		drt.setPlotDetailedCustomerStats(true);
		drt.setIdleVehiclesReturnToDepots(false);
		drt.setMaxTravelTimeAlpha(alpha);
		drt.setMaxTravelTimeBeta(beta);
		drt.setMaxWaitTime(maxWait);

//		drt.setRequestRejection(isEnableRequestRejection);

		Controler controler = DrtControlerCreator.createControler(config, false);
		controler.run();
	}

}
