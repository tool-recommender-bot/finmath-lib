package net.finmath.fouriermethod.calibration.models;

import net.finmath.fouriermethod.calibration.ScalarParameterInformation;
import net.finmath.fouriermethod.calibration.ScalarParameterInformationImplementation;
import net.finmath.fouriermethod.calibration.Unconstrained;
import net.finmath.fouriermethod.models.HestonModel;
import net.finmath.modelling.ModelDescriptor;
import net.finmath.modelling.descriptor.HestonModelDescriptor;

/**
 * This class is creates new instances of HestonModel and communicates with the optimization algorithm.
 *
 * This class provides clones of herself: in such a way the information concerning constraints is not lost.
 *
 * The method getCharacteristicFunction is then passed to the FFT pricing routine.
 *
 * @author Alessandro Gnoatto
 */
public class CalibrableHestonModel implements  CalibrableProcess {
	private final HestonModelDescriptor descriptor;

	private final ScalarParameterInformation volatilityInfo;
	private final ScalarParameterInformation thetaInfo;
	private final ScalarParameterInformation kappaInfo;
	private final ScalarParameterInformation xiInfo;
	private final ScalarParameterInformation rhoInfo;
	private final boolean applyFellerConstraint;

	/*
	 * Upper and lower bounds are collected here for convenience:
	 * such vectors are then passed to the factory of the optimization algorithm.
	 * In this way we guarantee consistency between the constraints in the model
	 * and the constraints in the optimizer factory.
	 */
	private final double[] parameterUpperBounds;
	private final double[] parameterLowerBounds;

	/**
	 * Basic constructor where all parameters are to be calibrated.
	 * All parameters are unconstrained.
	 *
	 * @param descriptor The model descriptor for the Heston model.
	 */
	public CalibrableHestonModel(HestonModelDescriptor descriptor) {
		super();
		this.descriptor = descriptor;
		this.volatilityInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		this.thetaInfo =new ScalarParameterInformationImplementation(true, new Unconstrained());
		this.kappaInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		this.xiInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		this.rhoInfo = new ScalarParameterInformationImplementation(true, new Unconstrained());
		this.applyFellerConstraint = false;
		this.parameterUpperBounds = extractUpperBounds();
		this.parameterLowerBounds = extractLowerBounds();
	}

	/**
	 * This constructor allows for the specification of constraints.
	 * This is very liberal since we can impose different types of constraints.
	 * The choice on the parameters to be applied is left to the user.
	 * This implies that he user could create Heston models which are not admissible in the sense of Duffie Filipovic and Schachermayer (2003).
	 * For example, it is up to the user to impose constraints such that the product of kappa and theta is positive.
	 *
	 * @param descriptor The model descriptor for the Heston model.
	 * @param volatilityConstraint The volatility constraint.
	 * @param thetaConstraint The constraint for the theta parameter.
	 * @param kappaConstraint The constraint for the kappa parameter.
	 * @param xiConstraint The constraint for the xi parameter.
	 * @param rhoConstraint The constraint for the rho parameter.
	 * @param applyFellerConstraint If true, the Feller constraint is applied.
	 */
	public CalibrableHestonModel(HestonModelDescriptor descriptor, ScalarParameterInformation volatilityConstraint,
			ScalarParameterInformation thetaConstraint, ScalarParameterInformation kappaConstraint, ScalarParameterInformation xiConstraint,
			ScalarParameterInformation rhoConstraint, boolean applyFellerConstraint) {
		this.descriptor = descriptor;
		this.volatilityInfo = volatilityConstraint;
		this.thetaInfo = thetaConstraint;
		this.kappaInfo = kappaConstraint;
		this.xiInfo = xiConstraint;
		this.rhoInfo = rhoConstraint;
		this.applyFellerConstraint = applyFellerConstraint;
		this.parameterUpperBounds = extractUpperBounds();
		this.parameterLowerBounds = extractLowerBounds();
	}

	@Override
	public CalibrableHestonModel getCloneForModifiedParameters(double[] parameters) {

		//If the parameters are to be calibrated we update the value, otherwise we use the stored one.
		double volatility = volatilityInfo.getIsParameterToCalibrate() == true ? volatilityInfo.getConstraint().apply(parameters[0]) : descriptor.getVolatility();
		double theta = thetaInfo.getIsParameterToCalibrate() == true ? thetaInfo.getConstraint().apply(parameters[1]) : descriptor.getTheta();
		double kappa = kappaInfo.getIsParameterToCalibrate() == true ? kappaInfo.getConstraint().apply(parameters[2]) : descriptor.getKappa();
		double xi = xiInfo.getIsParameterToCalibrate() == true ? xiInfo.getConstraint().apply(parameters[3]) : descriptor.getXi();
		double rho = rhoInfo.getIsParameterToCalibrate() == true ? rhoInfo.getConstraint().apply(parameters[4]) : descriptor.getRho();

		if(applyFellerConstraint && 2*kappa*theta < xi*xi) {
			//bump long term volatility so that the Feller test is satisfied.
			theta = xi*xi / (2 * kappa) + 1E-9;
		}else {
			//nothing to do;
		}

		HestonModelDescriptor newDescriptor = new HestonModelDescriptor(descriptor.getReferenceDate(),
				descriptor.getInitialValue(),descriptor.getDiscountCurveForForwardRate(), descriptor.getDiscountCurveForForwardRate(),
				volatility, theta, kappa, xi, rho);

		return new CalibrableHestonModel(newDescriptor,this.volatilityInfo,this.thetaInfo,this.kappaInfo,this.xiInfo,this.rhoInfo,this.applyFellerConstraint);
	}

	@Override
	public ModelDescriptor getModelDescriptor() {
		return descriptor;
	}

	@Override
	public HestonModel getCharacteristiFunction() {
		return new HestonModel(descriptor.getInitialValue(),descriptor.getDiscountCurveForForwardRate(),
				descriptor.getVolatility(),descriptor.getDiscountCurveForForwardRate(),
				descriptor.getTheta(),descriptor.getKappa(),descriptor.getXi(),descriptor.getRho());
	}

	@Override
	public double[] getParameterUpperBounds() {
		return parameterUpperBounds;
	}

	@Override
	public double[] getParameterLowerBounds() {
		return parameterLowerBounds;
	}

	private double[] extractUpperBounds() {
		double[] upperBounds = new double[5];
		double threshold = 1E6;
		upperBounds[0] = volatilityInfo.getConstraint().getUpperBound() > threshold ? threshold : volatilityInfo.getConstraint().getUpperBound();
		upperBounds[1] = thetaInfo.getConstraint().getUpperBound() > threshold ? threshold : thetaInfo.getConstraint().getUpperBound();
		upperBounds[2] = kappaInfo.getConstraint().getUpperBound() > threshold ? threshold : kappaInfo.getConstraint().getUpperBound();
		upperBounds[3] = xiInfo.getConstraint().getUpperBound() > threshold ? threshold : xiInfo.getConstraint().getUpperBound();
		upperBounds[4] = rhoInfo.getConstraint().getUpperBound() > threshold ? threshold : rhoInfo.getConstraint().getUpperBound();

		return upperBounds;
	}

	private double[] extractLowerBounds() {
		double[] upperBounds = new double[5];
		double threshold = -1E6;
		upperBounds[0] = volatilityInfo.getConstraint().getLowerBound() < threshold ? threshold : volatilityInfo.getConstraint().getLowerBound();
		upperBounds[1] = thetaInfo.getConstraint().getLowerBound() < threshold ? threshold : thetaInfo.getConstraint().getLowerBound();
		upperBounds[2] = kappaInfo.getConstraint().getLowerBound() < threshold ? threshold : kappaInfo.getConstraint().getLowerBound();
		upperBounds[3] = xiInfo.getConstraint().getLowerBound() < threshold ? threshold : xiInfo.getConstraint().getLowerBound();
		upperBounds[4] = rhoInfo.getConstraint().getLowerBound() < threshold ? threshold : rhoInfo.getConstraint().getLowerBound();

		return upperBounds;
	}

}
