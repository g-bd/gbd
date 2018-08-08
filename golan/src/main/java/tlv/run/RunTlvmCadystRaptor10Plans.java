package tlv.run;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Path;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.cadyts.car.CadytsCarModule;
import org.matsim.contrib.cadyts.car.CadytsContext;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.contrib.cadyts.general.CadytsScoring;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelOpenTimesScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParameters;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsReaderMatsimV1;
import org.matsim.roadpricing.ControlerDefaultsWithRoadPricingModule;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.VehicleWriterV1;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Provider;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;


/**
 * @author GBD
 * Run TLVM with Cadyst + roadpricing + subpopulation + raptor, each agent memory 10 plans
 *
 */
//test commot
public class RunTlvmCadystRaptor10Plans {
	//matsim uses the relative path for the path which the config files resides
	final static String INPUT_FOLDER = "src\\main\\resources\\tlvm_calibrated_raptor\\input\\";
	final static String OUTPUT_FOLDER = "tlvm_calibrated_raptor\\output\\";

	
	public static void main(String[] args) {

		double msa = 0.6333333333333333;
		double flowCapacity = 0.1;
		double pop = 0.1;
		double storagExpo = 0.75;
		double storageCapacity = Math.pow(flowCapacity, storagExpo);
		double pce = 0.1;
		// String fce = "off";

		Config config = ConfigUtils.loadConfig(INPUT_FOLDER + "newconfignostrategy.xml");
		
		String vehcileFileInput = changeVehcilePce(pce, pop);
		config.transit().setVehiclesFile(vehcileFileInput);

		//network capacity link l
		config.network().setInputFile("Network_9_model_4_walk_to_train_only_clean_modal_fix_capac.xml");
		config.global().setNumberOfThreads(15);
		config.qsim().setNumberOfThreads(15);
//		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(OUTPUT_FOLDER + "tlvm_flow_"+flowCapacity+"_storage_"+storageCapacity+"_counts_2013_sub_pop_300_iters_ChangeExpBeta_mse_fractions_"+msa+"_cadyt_raptor_10_plans");
		config.controler().setWritePlansInterval(100);
		config.controler().setWriteEventsInterval(100);
		config.controler().setLastIteration(300);
		config.facilities().setInputFile("facilities.xml");
		config.transit().setTransitScheduleFile("transitSchedule7_model_4.xml");
		// config.linkStats().setAverageLinkStatsOverIterations(5); // this is for link
		 config.qsim().setFlowCapFactor(flowCapacity);
		 config.qsim().setStorageCapFactor(storageCapacity);
		// config.qsim().setPcuThresholdForFlowCapacityEasing(0.3);
		config.counts().setInputFile("counts_2013_no_outliers.xml");


		// ***********************set plans and subopluation files*****************************
		config.plans().setInputFile("0.plans.xml.gz");
		config.plans().setSubpopulationAttributeName("subpopulation");
		config.plans().setInputPersonAttributeFile("personAtrributes-with-subpopulation.xml");
		//set MSA
		config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.6333333333333333);
		// ************************set general strategy*************************
		// max memory 10
		config.strategy().setMaxAgentPlanMemorySize(10);
		// innovation will be stop after iter * fraction . eg if 100 iters then at 80
		config.strategy().setFractionOfIterationsToDisableInnovation(0.6333333333333333);
		
		// ************************set internal sub-population strategy*************************
		StrategySettings changeExpBetaInternal = new StrategySettings();
		// plan selector - internal agents (without disable)
		changeExpBetaInternal.setDisableAfter(-1);
		changeExpBetaInternal.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpBetaInternal.setWeight(0.8);
		changeExpBetaInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(changeExpBetaInternal);

		// reroute internal agents
		StrategySettings reRouteInternal = new StrategySettings();
		reRouteInternal.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
		reRouteInternal.setWeight(0.1);
		reRouteInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(reRouteInternal);

		// time-mutation internal agents
		StrategySettings timeMutatorInternal = new StrategySettings();
		timeMutatorInternal.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
		timeMutatorInternal.setWeight(0.1);
		timeMutatorInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(timeMutatorInternal);
		
		// time-mutation_reroute internal comment the above when using this
/*		StrategySettings reRouteTimeMutatorInternal = new StrategySettings();
		reRouteTimeMutatorInternal.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator_ReRoute.toString());
		reRouteTimeMutatorInternal.setWeight(0.1);
		reRouteTimeMutatorInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(reRouteTimeMutatorInternal);*/

		// sub-tour mode chouce internal agents
		StrategySettings subTourModeChoiceInternal = new StrategySettings();
		subTourModeChoiceInternal.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.toString());
		subTourModeChoiceInternal.setWeight(0.1);
		subTourModeChoiceInternal.setSubpopulation("internalAgent");
		config.strategy().addStrategySettings(subTourModeChoiceInternal);

		// add innovative modules for external agents
		final String REROUTE_FOR_SUBPOP_EXTERNAL = DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.toString().concat("externalAgent");
		{
			StrategyConfigGroup.StrategySettings modeChoiceStrategySettings = new StrategyConfigGroup.StrategySettings();
			modeChoiceStrategySettings.setStrategyName(REROUTE_FOR_SUBPOP_EXTERNAL); 																					
			modeChoiceStrategySettings.setSubpopulation("externalAgent");
			modeChoiceStrategySettings.setWeight(0.1);
			config.strategy().addStrategySettings(modeChoiceStrategySettings);
			StrategyConfigGroup.StrategySettings changeExpBetaStrategySettings = new StrategyConfigGroup.StrategySettings();
			changeExpBetaStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
			changeExpBetaStrategySettings.setSubpopulation("externalAgent");
			changeExpBetaStrategySettings.setWeight(0.8);
			config.strategy().addStrategySettings(changeExpBetaStrategySettings);
		}


		// add road pricing
		RoadPricingConfigGroup rpConfig = ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class);
		rpConfig.setTollLinksFile("road6Toll.xml");

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(config);

		controler.setModules(new ControlerDefaultsWithRoadPricingModule());

		
		// make the controler react to facility open times:
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				CharyparNagelOpenTimesScoringFunctionFactory factory = new CharyparNagelOpenTimesScoringFunctionFactory(
						scenario);
				this.bindScoringFunctionFactory().toInstance(factory);
			}
		});

/*		 this is required to set the different set of available modes for second sub population example.
		(The name of the innovative module should be same as set in
		 config.strategy())*/
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addPlanStrategyBinding(REROUTE_FOR_SUBPOP_EXTERNAL).toProvider(new javax.inject.Provider<PlanStrategy>() {
							final String[] availableModes = { "car", "motorbike" };
							@Inject
							Scenario sc;
							@Inject
							Provider<TripRouter> tripRouterProvider;

							@Override
							public PlanStrategy get() {
								final PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
//								builder.addStrategyModule(new TripsToLegsModule(tripRouterProvider, sc.getConfig().global()));
								builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
								return builder.build();
							}
						});
			}
		});
        
		//cadyst
        CadytsConfigGroup cadystConfigGroup = ConfigUtils.addOrGetModule(config,  CadytsConfigGroup.class);
      cadystConfigGroup.setStartTime(6*60*60);
      cadystConfigGroup.setEndTime(72000);
//        cadystConfigGroup.setEndTime(108000);
//        cadystConfigGroup.setPreparatoryIterations(5);

        Counts<Link> calibrationCounts = new Counts<>();
        new CountsReaderMatsimV1(calibrationCounts).readFile(INPUT_FOLDER+"counts_2013_no_outliers.xml");
        // ---

        CadytsCarModule cadytsCarModule=new CadytsCarModule(calibrationCounts);  
        controler.addOverridingModule(cadytsCarModule);

		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Inject CadytsContext cadytsContext;
			@Inject ScoringParametersForPerson parameters;
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
				final ScoringParameters params = parameters.getScoringParameters(person);
				SumScoringFunction scoringFunctionAccumulator = new SumScoringFunction();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				final CadytsScoring<Link> scoringFunction = new CadytsScoring<>(person.getSelectedPlan(), config, cadytsContext);
				scoringFunction.setWeightOfCadytsCorrection(15. * config.planCalcScore().getBrainExpBeta()) ;
				scoringFunctionAccumulator.addScoringFunction(scoringFunction );

				return scoringFunctionAccumulator;
			}
		}) ;
		

		//Raptor PT default router without config
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				// To use the deterministic pt simulation:
//				install(new SBBQSimModule());

				// To use the fast pt router:
				install(new SwissRailRaptorModule());
			}
		});
		
		//adding determentisc raptor router 
/*		SBBTransitConfigGroup sbbConfig = ConfigUtils.addOrGetModule(config, SBBTransitConfigGroup.class);
		sbbConfig.setCreateLinkEventsInterval(100);
		Set<String> determansitcModes = new HashSet<>();
		determansitcModes.add("train");
		sbbConfig.setDeterministicServiceModes(determansitcModes);*/

		controler.run();
	}

	/*
	 * Change PT pce/capacity to match downscaled scenario 
	 */
	public static String changeVehcilePce(double pce, double pop) {
		// create veucile object
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();

		// input file
		new VehicleReaderV1(vehicles).readFile(INPUT_FOLDER + "Vehicles_4_model_4.xml");

		// loop on vechiles to change
		for (VehicleType vehcile : vehicles.getVehicleTypes().values()) {

			// if (!vehcile.getId().toString().startsWith("Bus")) {
			// get vehcile capacity total
			VehicleCapacity capacity = vehcile.getCapacity();
			// set seats with scaling
			capacity.setSeats((int) Math.ceil(capacity.getSeats() * pop));
			// set standing with scaling
			capacity.setStandingRoom((int) Math.ceil(capacity.getStandingRoom() * pop));
			// set PCE
			vehcile.setPcuEquivalents(vehcile.getPcuEquivalents() * pce);
			// }
		}

		String fileName = "Vehicles_4_model_4" + (int) (pce) + "pop_" + (int) (pop * 100) + "_pct.xml";
		new VehicleWriterV1(vehicles).writeFile(INPUT_FOLDER+fileName);
		return fileName;
	}
}
