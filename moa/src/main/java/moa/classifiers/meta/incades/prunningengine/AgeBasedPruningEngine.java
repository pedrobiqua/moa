package moa.classifiers.meta.incades.prunningengine;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.yahoo.labs.samoa.instances.Instance;

public class AgeBasedPruningEngine {
    private int maxPoolSize;
    public List<MeasuredClassifier> pruneClassifiers(MeasuredClassifier newClassifier,
			List<MeasuredClassifier> currentPool, List<Instance> accuracyEstimationInstances) {
		if(currentPool.size() + 1 <= maxPoolSize)//sum 1, since a new classifier (newClassifier) will be added in the pool
			return new ArrayList<MeasuredClassifier>();
		int numClassifiers = currentPool.size() + 1 - maxPoolSize;
		List<MeasuredClassifier> classifiesToPrune = 
				new ArrayList<MeasuredClassifier>(numClassifiers);
		SortedSet<Long> agesSet = new TreeSet<Long>();
		for(MeasuredClassifier ic : currentPool)
			agesSet.add(ic.getMetrics().getCreationTime());
		
		Long prunningAge = -1L;
		int agesChecked = 0;
		for(Long age : agesSet){
			prunningAge = age;
			agesChecked++;
			if(agesChecked == numClassifiers)
				break;
		}
		for(MeasuredClassifier dc : currentPool){
			if(dc.getMetrics().getCreationTime() <= prunningAge){
				classifiesToPrune.add(dc);
				if(classifiesToPrune.size() == numClassifiers)
					break;
			}
		}
		return classifiesToPrune;
	}
}
