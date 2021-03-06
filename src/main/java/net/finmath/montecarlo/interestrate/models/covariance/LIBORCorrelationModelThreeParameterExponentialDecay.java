/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import net.finmath.functions.LinearAlgebra;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * Simple correlation model given by R, where R is a factor reduced matrix
 * (see {@link net.finmath.functions.LinearAlgebra#factorReduction(double[][], int)}) created from the
 * \( n \) Eigenvectors of \( \tilde{R} \) belonging to the \( n \) largest non-negative Eigenvalues,
 * where \( \tilde{R} = \tilde{\rho}_{i,j} \) and
 * \[ \tilde{\rho}_{i,j} = b + (1-b) * \exp(-a |T_{i} - T_{j}| - c \max(T_{i},T_{j}))
 *
 * @see net.finmath.functions.LinearAlgebra#factorReduction(double[][], int)
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORCorrelationModelThreeParameterExponentialDecay extends LIBORCorrelationModel {

	private static final long serialVersionUID = 5063076041285957177L;

	private int		numberOfFactors;
	private double	a;
	private double	b;
	private double	c;
	private final boolean isCalibrateable;

	private Object lazyInitLock = new Object();	// lock used for lazy init of correlationMatrix and factorMatrix

	private transient double[][]	correlationMatrix;
	private transient double[][]	factorMatrix;

	public LIBORCorrelationModelThreeParameterExponentialDecay(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, int numberOfFactors, double a, double b, double c, boolean isCalibrateable) {
		super(timeDiscretization, liborPeriodDiscretization);

		this.numberOfFactors = numberOfFactors;
		this.a = a;
		this.b = b;
		this.c = c;
		this.isCalibrateable = isCalibrateable;
	}

	@Override
	public RandomVariable[] getParameter() {
		if(!isCalibrateable) {
			return null;
		}

		RandomVariable[] parameter = new RandomVariable[3];

		parameter[0] = new Scalar(a);
		parameter[1] = new Scalar(b);
		parameter[2] = new Scalar(c);

		return parameter;
	}

	@Override
	public LIBORCorrelationModelThreeParameterExponentialDecay getCloneWithModifiedParameter(RandomVariable[] parameter) {
		if(!isCalibrateable) {
			return this;
		}

		return new LIBORCorrelationModelThreeParameterExponentialDecay(
				getTimeDiscretization(),
				getLiborPeriodDiscretization(),
				getNumberOfFactors(),
				parameter[0].doubleValue(), parameter[1].doubleValue(), parameter[2].doubleValue(), isCalibrateable);
	}

	@Override
	public double	getFactorLoading(int timeIndex, int factor, int component) {
		synchronized (lazyInitLock) {
			if(factorMatrix == null) {
				initialize(numberOfFactors, a, b, c);
			}
		}

		return factorMatrix[component][factor];
	}
	@Override
	public double	getCorrelation(int timeIndex, int component1, int component2) {
		synchronized (lazyInitLock) {
			if(correlationMatrix == null) {
				initialize(numberOfFactors, a, b, c);
			}
		}

		return correlationMatrix[component1][component2];
	}

	@Override
	public int		getNumberOfFactors() {
		return numberOfFactors;
	}

	private void initialize(int numberOfFactors, double a, double b, double c) {
		/*
		 * Create instantaneous correlation matrix
		 */

		a = Math.max(a, 0.0);
		b = Math.min(Math.max(b, 0.0), 1.0);
		c = Math.max(c, 0.0);

		correlationMatrix = new double[liborPeriodDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int row=0; row<correlationMatrix.length; row++) {
			for(int col=row+1; col<correlationMatrix[row].length; col++) {
				// Exponentially decreasing instantaneous correlation
				double T1 = liborPeriodDiscretization.getTime(row);
				double T2 = liborPeriodDiscretization.getTime(col);
				double correlation = b + (1-b) * Math.exp(-a * Math.abs(T1 - T2) - c * Math.max(T1, T2));
				correlationMatrix[row][col] = correlation;
				correlationMatrix[col][row] = correlation;
			}
			correlationMatrix[row][row] = 1.0;
		}

		/*
		 * Perform a factor decomposition (and reduction if numberOfFactors < correlationMatrix.columns())
		 */
		factorMatrix = LinearAlgebra.factorReduction(correlationMatrix, numberOfFactors);

		for(int component1=0; component1<factorMatrix.length; component1++) {
			for(int component2=component1+1; component2<factorMatrix.length; component2++) {
				double correlation = 0.0;
				for(int factor=0; factor<factorMatrix[component1].length; factor++) {
					correlation += factorMatrix[component1][factor] * factorMatrix[component2][factor];
				}
				correlationMatrix[component1][component2] = correlation;
				correlationMatrix[component2][component1] = correlation;
			}
			correlationMatrix[component1][component1] = 1.0;
		}
	}

	@Override
	public Object clone() {
		initialize(numberOfFactors, a, b, c);

		LIBORCorrelationModelThreeParameterExponentialDecay newModel = new LIBORCorrelationModelThreeParameterExponentialDecay(
				super.getTimeDiscretization(),
				super.getLiborPeriodDiscretization(),
				numberOfFactors,
				a, b, c, isCalibrateable);

		newModel.correlationMatrix	= this.correlationMatrix;
		newModel.factorMatrix		= this.factorMatrix;

		return newModel;
	}
}
