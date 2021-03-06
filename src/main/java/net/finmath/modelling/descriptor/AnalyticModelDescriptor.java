package net.finmath.modelling.descriptor;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.volatilities.VolatilitySurface;

/**
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class AnalyticModelDescriptor implements InterestRateModelDescriptor {

	private final LocalDate referenceDate;
	private final Map<String, Curve>				curvesMap;
	private final Map<String, VolatilitySurface>	volatilitySurfaceMap;

	/**
	 * Construct an AnalyticModelDescriptor mapping the collections of curves and volatility surfaces provided.
	 *
	 * @param referenceDate The date corresponding to time \( t = 0 \).
	 * @param curves The collection of curves.
	 * @param surfaces The collection of volatility surfaces.
	 */
	public AnalyticModelDescriptor(LocalDate referenceDate, Collection<Curve> curves, Collection<VolatilitySurface> surfaces) {
		super();
		this.referenceDate = referenceDate;

		curvesMap = new HashMap<String, Curve>();
		volatilitySurfaceMap = new HashMap<String, VolatilitySurface>();

		if(curves != null) {
			for(Curve curve : curves) {
				curvesMap.put(curve.getName(), curve);
			}
		}
		if (surfaces != null) {
			for (VolatilitySurface surface : surfaces) {
				volatilitySurfaceMap.put(surface.getName(), surface);
			}
		}
	}

	/**
	 * Construct an AnalyticModelDescriptor holding copies of the maps provided.
	 *
	 * @param referenceDate The date corresponding to time \( t = 0 \).
	 * @param curvesMap The map of curves.
	 * @param volatilitySurfaceMap The map of volatility surfaces.
	 */
	public AnalyticModelDescriptor(LocalDate referenceDate, Map<String, Curve> curvesMap, Map<String, VolatilitySurface> volatilitySurfaceMap) {
		super();
		this.referenceDate = referenceDate;

		this.curvesMap = new HashMap<String, Curve>();
		this.volatilitySurfaceMap = new HashMap<String, VolatilitySurface>();

		this.curvesMap.putAll(curvesMap);
		this.volatilitySurfaceMap.putAll(volatilitySurfaceMap);
	}

	@Override
	public Integer version() {
		return 1;
	}

	@Override
	public String name() {
		return "Analytic model";
	}

	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	public Map<String, Curve> getCurvesMap() {
		return Collections.unmodifiableMap(curvesMap);
	}

	public Map<String, VolatilitySurface> getVolatilitySurfaceMap() {
		return Collections.unmodifiableMap(volatilitySurfaceMap);
	}
}
