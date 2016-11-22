package com.zodiackillerciphers.homophones;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.zodiackillerciphers.ciphers.Ciphers;
import com.zodiackillerciphers.io.FileUtil;
import com.zodiackillerciphers.ngrams.Periods;
import com.zodiackillerciphers.old.CombinationGenerator;
import com.zodiackillerciphers.tests.jarlve.JarlveMeasurements;
import com.zodiackillerciphers.transform.CipherTransformations;

public class HomophonesNew extends Thread {

	
	//String ciphertext; HomophoneSequenceBean bean; Set<Character> set;
	
	public static List<HomophonesResultBean> z340beans;
	public static float[] z340scores;
	static {
		String cipher = Ciphers.cipher[0].cipher;
		z340beans = search(cipher, Ciphers.alphabetAsArray(cipher), 2);
		z340scores = bestScoresPerSymbol(z340beans, 10);
	}
	
	/**
	 * find all homophone candidates and rank them by a simple probability
	 * estimate
	 */
	public static List<HomophonesResultBean> search(String ciphertext,
			String[] alphabet, int n) {
		return search(ciphertext, alphabet, n, true, true, 1, null);
	}
	
	public static List<HomophonesResultBean> search(String ciphertext,
			String[] alphabet, int n, boolean computeProbabilities, boolean sort, int minRunLength, PerfectCyclesBean pcb) {
		if (ciphertext == null)
			return null;
		if (alphabet == null)
			return null;
		List<HomophonesResultBean> beans = new ArrayList<HomophonesResultBean>();

		Map<Character, Integer> counts = Ciphers.countMap(ciphertext);

		// compute all combinations of n symbols selected from the given
		// alphabet.
		int[] indices;
		if (n > alphabet.length || alphabet.length < 1) return beans;
		CombinationGenerator x = new CombinationGenerator(alphabet.length, n);
		StringBuffer combination;
		while (x.hasMore()) {
			combination = new StringBuffer();
			indices = x.getNext();
			for (int i = 0; i < indices.length; i++) {
				combination.append(alphabet[indices[i]]);
			}
			String key = combination.toString();
			if (key.contains(" ")) continue; // ignore spaces
			HomophoneSequenceBean fullsequence = isolateSequence(ciphertext, key);
			HomophonesResultBean bestCycle = bestCycleIn(fullsequence, n);
			if (computeProbabilities) bestCycle.computeProbability(ciphertext.length(), counts);
			if (bestCycle.getRun() >= minRunLength) 
			beans.add(bestCycle);
		}
		if (sort) Collections.sort(beans, new Comparator<HomophonesResultBean>() {
			@Override
			public int compare(HomophonesResultBean o1, HomophonesResultBean o2) {
				return Float.compare(o1.getRunProbability(),
						o2.getRunProbability());
			}
		});
		
		if (pcb != null) { 
			pcb.score = perfectCycleScoreFor(n, 3, beans);
			//System.out.println(pcb.score);
		}
		return beans;
	}

	/** return top N per-symbol cycle scores. */
	public static float[] bestScoresPerSymbol(
			List<HomophonesResultBean> beans, int N) {
		Map<Character, Float> map = new HashMap<Character, Float>();
		for (HomophonesResultBean bean : beans) {
			for (int i = 0; i < bean.getSequence().length(); i++) {
				char key = bean.getSequence().charAt(i);
				Float val = map.get(key);
				if (val == null)
					val = bean.simpleScore();
				else if (bean.simpleScore() > val) {
					val = bean.simpleScore();
				}
				map.put(key, val);
				//System.out.println("put " + key + " " + val);
			}
		}
		
		Float[] unsorted = map.values().toArray(new Float[0]);
		Arrays.sort(unsorted);
		float[] result = new float[unsorted.length];
		for (int i=0; i<N; i++) result[i] = unsorted[unsorted.length-i-1];
		return result;
	}
	
	public static Map<Character, Float> lowestProbsPerSymbol(
			List<HomophonesResultBean> beans) {
		Map<Character, Float> map = new HashMap<Character, Float>();
		for (HomophonesResultBean bean : beans) {
			for (int i = 0; i < bean.getSequence().length(); i++) {
				char key = bean.getSequence().charAt(i);
				Float val = map.get(key);
				if (val == null)
					val = bean.getRunProbability();
				else if (bean.getRunProbability() < val) {
					//System.out.println(bean.getProbability() + " is better than " + val + " for " + key);
					val = bean.getRunProbability();
				}
				map.put(key, val);
				//System.out.println("put " + key + " " + val);
			}
		}
		return map;
	}
	public static Map<Character, Integer> maxRunsPerSymbol(
			List<HomophonesResultBean> beans) {
		Map<Character, Integer> map = new HashMap<Character, Integer>();
		for (HomophonesResultBean bean : beans) {
			if (bean == null) throw new RuntimeException("WTf, bean is null");
			if (bean.getSequence() == null) throw new RuntimeException("WTf, bean sequence is null " + bean);
			for (int i = 0; i < bean.getSequence().length(); i++) {
				char key = bean.getSequence().charAt(i);
				Integer val = map.get(key);
				if (val == null)
					val = bean.run;
				else if (bean.run > val) {
					val = bean.run;
				}
				map.put(key, val);
			}
		}
		return map;
	}

	public static HomophoneSequenceBean isolateSequence(String ciphertext, String sequence) {
		HomophoneSequenceBean bean = new HomophoneSequenceBean();
		Set<Character> set = new HashSet<Character>();
		for (int i = 0; i < sequence.length(); i++)
			set.add(sequence.charAt(i));
		//String result = "";
		for (int i = 0; i < ciphertext.length(); i++) {
			char ch = ciphertext.charAt(i);
			if (set.contains(ch)) {
				bean.fullSequence += ch;
				bean.positions.add(i);
			}
		}
		return bean;
	}

	/*public HomophonesNew(String ciphertext, HomophoneSequenceBean bean, Set<Character> set) {
		this.ciphertext = ciphertext;
		this.bean = bean;
		this.set = set;
	}*/
	/* doesn't work public static HomophoneSequenceBean isolateSequenceMultithreaded(int numthreads, String ciphertext, String sequence) {

		Set<Character> set = new HashSet<Character>();
		for (int i = 0; i < sequence.length(); i++)
			set.add(sequence.charAt(i));
		
		List<Thread> threads = new ArrayList<Thread>();
		for (int i=0; i<numthreads; i++) {
			HomophoneSequenceBean bean = new HomophoneSequenceBean();
			String sub = ciphertext.substring(i*ciphertext.length()/numthreads, ((i+1)*340/numthreads));
			threads.add(new HomophonesNew(sub, bean, set));
		}
		
		for (Thread thread: threads) thread.run();
		for (Thread thread: threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		HomophoneSequenceBean bean = new HomophoneSequenceBean();
		
		for (Thread thread : threads) {
			HomophonesNew h = (HomophonesNew) thread;  
			bean.fullSequence += h.bean.fullSequence;
			bean.positions.addAll(h.bean.positions);
		}
		return bean;
	}*/

	/** doesn't work
	public void run() {
		System.out.println(ciphertext + "...");
		for (int i = 0; i < ciphertext.length(); i++) {
			char ch = ciphertext.charAt(i);
			if (set.contains(ch)) {
				bean.fullSequence += ch;
				bean.positions.add(i);
			}
		}
	}*/
	

	/** look for the best unbroken run of cycles */
	public static HomophonesResultBean bestCycleIn(HomophoneSequenceBean sequence, int n) {
		if (sequence == null)
			return null;
		if (sequence.fullSequence.length() < n)
			return null;
		Map<String, Integer> counts = new HashMap<String, Integer>();
		Map<String, Integer> runs = new HashMap<String, Integer>();
		String bestSequence = null;
		int bestCount = 0;

		int bestRun = 0;
		String bestRunKey = null;

		for (int i = 0; i < sequence.fullSequence.length() - n + 1; i++) {
			String key = sequence.fullSequence.substring(i, i + n);
			if (unique(key) != n)
				continue;
			Integer val = counts.get(key);
			if (val == null)
				val = 0;
			val++;
			counts.put(key, val);
			if (val > bestCount) {
				bestSequence = key;
				bestCount = val;
			}

			// check for contiguous run for current sequence
			if (i >= n) {
				String keyPrev = sequence.fullSequence.substring(i - n, i);
				if (keyPrev.equals(key)) {
					Integer run = runs.get(key);
					if (run == null)
						run = 0;
					run++;
					runs.put(key, run);
					if (run > bestRun) {
						bestRun = run;
						bestRunKey = key;
					}
				} else {
					runs.put(key, 0);
				}
			}
		}

		HomophonesResultBean bean = new HomophonesResultBean(bestSequence,
				bestCount, 0f, sequence);
		bean.setRun(bestRun + 1);
		bean.setRunSequence(bestRunKey == null ? bestSequence : bestRunKey);
		return bean;

	}

	public static int unique(String str) {
		Set<Character> set = new HashSet<Character>();
		for (int i = 0; i < str.length(); i++)
			set.add(str.charAt(i));
		return set.size();
	}

	/** mean of N most rare length L cycle candidates */
	public static float measure(String ciphertext, int L, int N) {
		String a = Ciphers.alphabet(ciphertext);
		String[] alphabet = new String[a.length()];
		for (int i = 0; i < a.length(); i++)
			alphabet[i] = "" + a.charAt(i);
		List<HomophonesResultBean> beans = search(ciphertext, alphabet, L);
		return 0f;
	}

	public static void testShuffles() {
		float target = (float) 2.4584872E-24;
		int L = 2;
		String cipher = Ciphers.cipher[0].cipher;
		int hits = 0;
		int count = 0;
		for (int n = 0; n < 10000000; n++) {
			cipher = CipherTransformations.shuffle(cipher);
			String a = Ciphers.alphabet(cipher);
			String[] alphabet = new String[a.length()];
			for (int i = 0; i < a.length(); i++)
				alphabet[i] = "" + a.charAt(i);
			List<HomophonesResultBean> beans = search(cipher, alphabet, L);
			// for (HomophonesResultBean bean : beans)
			// System.out.println(bean.toString());
			float sum = 0;
			float min = Float.MAX_VALUE;
			float max = 0;
			for (HomophonesResultBean bean : beans)
				min = Math.min(min, bean.getRunProbability());

			/*
			 * Map<Character, Float> map = lowestProbsPerSymbol(beans); for
			 * (Character key : map.keySet()) { float val = map.get(key); sum +=
			 * val; if (val < min) min = val; if (val > max) max = val;
			 * System.out.println("Min prob for symbol " + key + " is " + val);
			 * } System.out.println("Per symbol prob sum: " + sum);
			 */
			// System.out.println(count);
			count++;
			if (min <= target) {
				hits++;
				System.out.println("hits " + hits + " count " + count
						+ " cipher " + cipher);
				// System.out.println(beans.get(0).toString());
				// System.out.println("mean " + (sum/map.size()) + " min " + min
				// + " max " + max);
				// break;
			}
		}
	}

	
	public static void testShuffleUntilTargetFound(float target, boolean evenOddTesting) {
		int L = 2;
		String cipher = Ciphers.cipher[0].cipher;
		int count = 0;
		while (true) {
			cipher = CipherTransformations.shuffle(cipher);
			String a = Ciphers.alphabet(cipher);
			String[] alphabet = new String[a.length()];
			for (int i = 0; i < a.length(); i++)
				alphabet[i] = "" + a.charAt(i);
			List<HomophonesResultBean> beans = search(cipher, alphabet, L);
			float min = Float.MAX_VALUE;
			float max = 0;
			for (HomophonesResultBean bean : beans)
				min = Math.min(min, bean.getRunProbability());

			Map<Character, Float> map = lowestProbsPerSymbol(beans);
			for (Character key : map.keySet()) {
				float val = map.get(key);
				if (val < min)
					min = val;
				if (val > max)
					max = val;
			}
			count++;
			if (min <= target) {
				
				boolean found = false;
				for (HomophonesResultBean bean : beans) {
					if (evenOddTesting) {
						if (bean.fullsequence.isAllEven() || bean.fullsequence.isAllOdd()) {
							found = true;
							System.out.println(bean.toString());
						}
					}
					else System.out.println(bean.toString());
				}
				if (!evenOddTesting || found) {
					for (Character key : map.keySet()) {
						float val = map.get(key);
						System.out.println(key + " " + val); // min probabilities
																// per symbol
					}
					System.out.println("Found in " + count + " shuffles.");
					break;
				}
			}
		}
	}

	public static void testShufflesWithAllEvensOrOddsFound(int runs) {
		int L = 2;
		String cipher = Ciphers.cipher[0].cipher;
		int count = 0;
		while (true) {
			count++;
			cipher = CipherTransformations.shuffle(cipher);
			String a = Ciphers.alphabet(cipher);
			String[] alphabet = new String[a.length()];
			for (int i = 0; i < a.length(); i++)
				alphabet[i] = "" + a.charAt(i);
			List<HomophonesResultBean> beans = search(cipher, alphabet, L);
			float min = Float.MAX_VALUE;
			float max = 0;
			for (HomophonesResultBean bean : beans) {
				if (bean.fullsequence.isAllEven() || bean.fullsequence.isAllOdd()) {
					if (bean.run >= runs) {
						System.out.println(count +" " + bean);
						return;
					}
				}
			}
		}
	}
	
	/** compute per-symbol scores for L=2.  sort by score and take the top 10.
	 * compare to the top 10 from z340, and return the difference in scores as the measurement.
	 */
	public static float measureCompare(String cipher) {
		float result = 0;
		int L = 2;
		int N = 10;
		List<HomophonesResultBean> beans = search(cipher, Ciphers.alphabetAsArray(cipher), L);
		float[] scores = bestScoresPerSymbol(beans, N);
		
		for (int i=0; i<N; i++) {
			if (i<scores.length) result += Math.abs(scores[i]-z340scores[i]);
			else result += z340scores[i];
		}
		
		return result;
	}

	/** given two sorted result beans, and starting from the beginning of each list,
	 * return how many entries in beans1 beat those in beans2. 
	 */
	public static int compare(List<HomophonesResultBean> beans1, List<HomophonesResultBean> beans2) {
		
		int count = 0;
		for (int i=0; i<beans1.size() && i<beans2.size(); i++) {
			if (beans1.get(i).runProbability >= beans2.get(i).runProbability) break;
			count++;
		}
		return count;
	}

	/**
	 * compute a composite score based on how perfect the cycles appear. and
	 * longer reps are weighted exponentially.
	 * 
	 * first, runs are tracked by a key.  the key is equal to the run length (e.g.: AB AB = 2).
	 * the value mapped to the run is the sum of the "percent" measurement which indicates how perfect the run is.
	 * example:  AB AB AA has run of 2, so its run of 4 symbols covers 6 possible symbols.  4/6 = 67%.
	 * imperfections are exaggerated by raising the percentage to the third power before adding.
	 * 
	 * the composite score is equal to the "sum of sums".  the run length determines a factor of 2 
	 * that is multiplied to the sum before adding, to give greater weight to longer runs.     
	 * 
	 */
	public static double perfectCycleScoreFor(int L, int minReps, List<HomophonesResultBean> beans) {
		boolean showSteps = false;
		double score = 0;
		Map<Integer, Double> byRun = new HashMap<Integer, Double>(); 
		if (beans == null) return score;
		for (HomophonesResultBean bean : beans) {
			int run = bean.run;
			if (run < minReps) continue;
			
			Double val = byRun.get(run);
			if (val == null) val = 0d;
			
			float percent = bean.runPercent();
			val += Math.pow(percent, 3); // exaggerate imperfections
			byRun.put(run, val);
			
			if (showSteps) {
				System.out.println(bean.sequence + " run " + bean.run + " percent " + percent + " Math.pow(percent, 3) " + Math.pow(percent, 3));
			}
		}
		
		for (Integer run : byRun.keySet()) {
			int exp = run-minReps+1;
			double add = byRun.get(run) * Math.pow(2, exp);
			if (showSteps) {
				System.out.println("run " + run + " val " + byRun.get(run) + " add " + add + " exp " + (run-minReps+1));
			}
			score += add;
		}
		return score;
	}
	public static double perfectCycleScoreFor(int L, String cipher, int minReps) {
		List<HomophonesResultBean> beans = beansFor(L, cipher);
		return perfectCycleScoreFor(L, minReps, beans);
	}
	
	/** get sorted result beans for given cipher */
	public static List<HomophonesResultBean> beansFor(int L, String cipher) {
		String a = Ciphers.alphabet(cipher.replaceAll(" ", ""));
		String[] alphabet = new String[a.length()];
		for (int i = 0; i < a.length(); i++)
			alphabet[i] = "" + a.charAt(i);
		List<HomophonesResultBean> beans = new ArrayList<HomophonesResultBean>();
		try {
			beans = search(cipher, alphabet, L);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return beans;
	}

	public static void test(int L, String cipher) {
		//String cipher = "HER>pl^VPk|1LTG2dJHz#L)(WGZU+Mc:yBU+R/5tE|DYBpbTMKONp+B(#O%DWY.<*Kf)2<clRJ|*5T4M.+&BFz69Sy#+N|5FBc(;8R";
		//String cipher = ">OpW%l+(D^k+)WV9/RLYPFMSK#.k2l+p2z<|NUOzpBH*1AYc-t7yJKL-6zX*j^:NfTld4+Gddlcp)GtU<K@VC|8M+H25EVM(L.k5*+BEdBT|++G9zFFVU(Rtc4D^b2#L>P3Z#l+(MYJ+J5|2+pG1G+;.B+Zf+pD&OK*Fz8+pORjK8(4+(:N6R&bp2#qR_TFO4^92BT7FO%^C/lp9fS<FM<B+;Fc(W^C5ycUKFc_Nk+)B.E2#l+OBypd+p|f>4+RR-ykW)R)MVbNJ/zS<WcLqU.||5z7CT|GZc5*Ztz+c25VFOBWL.y-48_c13B+AYP6zX|OOCBKBS<>;*HM+-D|H";
		boolean is408 = false;
		if (cipher.equals(Ciphers.cipher[1].cipher)) is408 = true;
		int[] numeric = null;
		Map<Character, Integer> numericMap = null; 
		if (is408) {
			numeric = Ciphers.toNumeric(cipher, false);
			numericMap = new HashMap<Character, Integer>();
			for (int i=0; i<numeric.length; i++) {
				numericMap.put(cipher.charAt(i), numeric[i]);
			}
		}
		String a = Ciphers.alphabet(cipher);
		String[] alphabet = new String[a.length()];
		for (int i = 0; i < a.length(); i++)
			alphabet[i] = "" + a.charAt(i);
		
		Long startTime = new Date().getTime();
		List<HomophonesResultBean> beans = search(cipher, alphabet, L, true, true, 2, null);
		System.out.println("elapsed: " + (new Date().getTime() - startTime));

		
		int allOdd = 0;
		int allEven = 0;
		
		for (HomophonesResultBean bean : beans) {
			String line = bean.toString();
			if (is408) {
				
				
				String numericSequence = "";
				Set<Character> plain = new HashSet<Character>();
				for (int i=0; i<bean.sequence.length(); i++) {
					plain.add(Ciphers.decoderMap.get(bean.sequence.charAt(i)));
					numericSequence += numericMap.get(bean.sequence.charAt(i)) + " ";
				}
				line = "'" + numericSequence + "	" + line;
				line += "	" + (plain.size() == 1 ? "TRUE CYCLE" : "FALSE CYCLE");
			}
			System.out.println(line);
			System.out.println(bean.positions(cipher));
			if (bean.fullsequence.isAllEven()) allEven++;
			else if (bean.fullsequence.isAllOdd()) allOdd++;
		}
		float sum = 0;
		float min = Float.MAX_VALUE;
		float max = 0;
		for (HomophonesResultBean bean : beans)
			min = Math.min(min, bean.getRunProbability());

		
		Map<Character, Float> map = lowestProbsPerSymbol(beans);
		for (Character key : map.keySet()) {
			float val = map.get(key);
			sum += val;
			if (val < min)
				min = val;
			if (val > max)
				max = val;
			System.out.println(key + " " + val); // min probabilities per symbol
		}
		System.out.println(allEven + "	" + allOdd);
		System.out.println("perfectCycleScore: " + perfectCycleScoreFor(L, cipher, 3));
		System.out.println("jarlve hom score (L=2): " + JarlveMeasurements.homScore(cipher));
		System.out.println("jarlve nonrepeat score: " + JarlveMeasurements.nonrepeat(cipher));
		System.out.println("jarlve nonrepeatAlternate score: " + JarlveMeasurements.nonrepeatAlternate(cipher));
		System.out.println("jarlve nonrepeatNormalizedAlternate score: " + JarlveMeasurements.nonrepeatNormalizedAlternate(cipher));
		//System.out.println("Per symbol prob sum: " + sum);
		//count++;
		//System.out.println("Count " + count);
	}

	public static void testZ408Plaintext(int L) {
		String plain = Ciphers.cipher[1].solution;
		String cipher = Ciphers.cipher[1].cipher;
		
		String a = Ciphers.alphabet(plain);
		String[] alphabet = new String[a.length()];
		for (int i = 0; i < a.length(); i++)
			alphabet[i] = "" + a.charAt(i);
		
		Long startTime = new Date().getTime();
		List<HomophonesResultBean> beans = search(plain, alphabet, L, true, true, 2, null);
		System.out.println("elapsed: " + (new Date().getTime() - startTime));

		
		for (HomophonesResultBean bean : beans) {
			System.out.println(bean.toString());
			
			Set<Character> symbols = new HashSet<Character>();
			for (int i=0; i<bean.sequence.length(); i++) {
				symbols.add(bean.sequence.charAt(i));
			}
			
			String isolated = "";
			for (int i=0; i<plain.length(); i++) {
				if (symbols.contains(plain.charAt(i))) {
					isolated += cipher.charAt(i);
				}
			}
			List<HomophonesResultBean> beans2 = search(isolated, Ciphers.alphabetAsArray(isolated), L, true, true, 2, null);
			System.out.println("		Symbols assigned to this plaintext sequence: " + isolated);
			for (HomophonesResultBean bean2 : beans2) {
				System.out.println("		- " + bean2);
			}
		}
	}
	
	
	/** determine how many results are equal or less than the given probability */ 
	public static void testCountProbs(float[] targets) {
		
		BufferedReader input = null;
		int[] hits = new int[targets.length];
		for (int i=0; i<targets.length; i++) hits[i] = 0;
		int counter = 0;
		try {
			input = new BufferedReader(new FileReader(new File("/Users/doranchak/projects/zodiac/l3")));
			String line = null; // not declared within while loop
			while ((line = input.readLine()) != null) {
				if (line.startsWith("Per symbol")) continue;
				if (line.startsWith("Count")) continue;
				String[] split = line.split(" ");
				String symbol = split[0];
				float prob = Float.valueOf(split[1]);
				for (int i=0; i<targets.length; i++) {
					if (prob <= targets[i]) hits[i]++;
				}
				counter++;
			}
			//System.out.println("read " + counter + " lines.");
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} 
		
		try {
			input.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		for (int i=0; i<targets.length; i++) {
			System.out.println("Target " + targets[i] + " Hits " + hits[i] + " Total " + counter + " Ratio " + ((float)hits[i])/counter);
		}
	}

	public static void testProcess() {
		Map<String, DescriptiveStatistics> stats = new HashMap<String, DescriptiveStatistics>();
		
		BufferedReader input = null;
		int counter = 0;
		try {
			input = new BufferedReader(new FileReader(new File("/Users/doranchak/projects/zodiac/shuffle-z408-l3")));
			String line = null; // not declared within while loop
			while ((line = input.readLine()) != null) {
				if (line.startsWith("Per symbol")) continue;
				if (line.startsWith("Count")) continue;
				String[] split = line.split(" ");
				String symbol = split[0];
				float prob = Float.valueOf(split[1]);
				
				DescriptiveStatistics stat = stats.get(symbol);
				if (stat == null) stat = new DescriptiveStatistics();
				stat.addValue(prob);
				stats.put(symbol, stat);
				counter++;
			}
			//System.out.println("read " + counter + " lines.");
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} 
		
		try {
			input.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		// show mean, variance, std dev for each
		System.out.println("symbol n min max mean variance stddev");
		for (String symbol : stats.keySet()) {
			DescriptiveStatistics stat = stats.get(symbol);
			System.out.println(symbol + " " + stat.getN() + " " + stat.getMin() + " " + stat.getMax() + " " + stat.getMean() + " " + stat.getVariance() + " " + stat.getStandardDeviation());
		}
		
	}
	
	public static void testVariousCiphers() {
		int L = 2;
		String cipher = Ciphers.cipher[0].cipher;
		System.out.println("=========================== Z340"); test(L, cipher); 
		//cipher = Ciphers.cipher[1].cipher;
		//System.out.println("=========================== Z408"); test(L, cipher); 
		/*cipher = "K)IM3$Um8lnJ#X5-*-MTCgN(96AcPgna$HaRY6cWb0*dN4LCAORc4_RejkHFDE3#KpnAPIH!M(TUS7*50*mHaMbQd*RK4iXHVAL$AGRQXCSa#k-&LK02aiIW1TK7!QJpUmMbH9nH!T*_(OfRH@7lQbHBkSC4K67HSR-gQ3Rc561-ZN(RDd9A%X_VSUhJaRg7&XW4Mn5hRT_m*3D$IHgo!X54b*8T6cpYEHml%Cabk*I@0U9oA#$4G0QX$SW_9E3A(FUCjYnhHdKS0H4m6*CBn-IQMkg-A6(*nH!aLm4GX5JU01CPDZMb4AXH8k*n%d_Vb*F_)mX9#$B6*&%D$TF)";
		System.out.println("=========================== chris cactus cipher"); test(L, cipher); 
		cipher = "++t4Vc.b425f^>REHR8;(cBF5|N+#yB+pNFB&+.M4T5*|JRc:yBOKMTbpBYD|Et57ppS-yBF<7pO+J^+V+M9_K46AycBF2RZ+b^R8p9L@+zYN_+O#jfK+5#|Lz.VGXcU2;%qJ2G((D2>FkCd*-OlF+;K|A8OZzSkpN+M<d>MDH/k4&+PF5|djtzcC-*BOY_Bt7<WUlz-|Fkd2KR++Op3V*8l^p)(/THSOPcWzC/R+U++)WJHz#L)(WGZU+ML)|BWlF+<C61Llc<2RcT+)fK*<.YWD%O#(2GqMf.^pO(KBzS96z|c.3d2GTL1|kPV^lp+-5ZUV>EC94:*NFGlyBX1";
		System.out.println("=========================== 40 bigrams from recent quadrant experiment"); test(L, cipher); 
		cipher = "2<cl>MDHU+R/|Fkd-zlU++)Wd<M+RcT+(G2J|c.3#5+KyBX1p8R^lGFN_9M+z69SSpp7By:cNp+BHER>RJ|*5T4M.+&BFNpkSzZO8A|K;+5tE|DYBpbTMKOW<7tB_YOB*-CcV+^J+Op7<FBy-CzWcPOSHT/()pb+ZR2FBcyA64KL16C<+FlWB|)Lfj#O+_NYz+@L9zBK(Op^.fMqG2q%;2UcXGV.zL|*:49CE>VUZ5-+FlO-*dCkF>2D(^f524b.cV4t++ztjd|5FP+&4k/y#+N|5FBc(;8R^l8*V3pO++RK2M+UZGW()L#zHJ(#O%DWY.<*Kf)pl^VPk|1LTG2d";
		System.out.println("=========================== 5 trigrams from recent quadrant experiment"); test(L, cipher); 
		cipher = "3:aON;653<!%C>.\\7#8Z<L6O\\Y?U:AO#7Y=(UB8,^aP+7/a>S#IOY5W;AOGYQ2C_Q3OF[+7PGU]N74()1\\6'\\*@M153\\86O&HRIL;3]4X56NE+*M187!Y<N[7\"\\;YMK\\3\\]6<&HR#Y.+X(>Y#]N\"(N7O3_9O'#P(M12Y+Y^>Q77>KCa4K8$3/,=(UB/,$;I]S-CO0;ND+YEQX;-BQ5\"<;Y(50>+.^18%<SO<Y\"Y3\\<,7<6*]V,/YL]B8#CF[,7_\\M:Q7`+I1DZ:\"7KO1>P5]Y<`[3O8,F*W5'J5\\)`'VZ<X*G05.BP<!(T8*\\^-+XC18%OVU\\,MG:DF(NI/V.Oa6";
		System.out.println("=========================== gardibolt"); test(L, cipher); 
		cipher = "XN*T-d>+dy+4C./1tFJH3(^pTCF+FB_+DL>+dRO5OBc^@.p5#L|4+pW)S_2+qtD+^<#TY5c|N.DXLEb-R5;cO<k5&Rl^yWRG2f)OOzV+2l)p9*_f.HFl<+kF+.%98*Ap5YZW+dfp76MMjBBlFO-U(BZL%BUR26LF5J8*S1R:N/kl2-q<SYbAp>)zKWVc<ky+7+Jy#U1zk+lOz#9:pZ+4+V;MlGLK#+GRJUB7M(D|z&FzT|+.KZ42)2V^CG+8(pY6MMUz39(fyM*2+HB+(2p|Cc+FBOztOK4+/VbE|RF|(zTp|8<P^W>KG*B-OCKSj|NcdBK|EGtc4PBPcNHcVc;W";
		System.out.println("=========================== best random shuffle from fragments experiment"); test(L, cipher); 
		cipher = "I4X91iZg;`GJ=b=7[2j2fKG8OPEUH6B:dXFXLfXmaJJ0QdWATXPkKZUHkl@ZHnCK4[TJA0Qnnd[gnOSK6CM\7[U0VL32G415Y4Y9[7KT[J=?dM9;`O5=kM8bK4_CfDT=m5G5IkY4VDFcWZB`TEFcXb67fKTJ9IQ25iVJ0?diZQ]hDLQ;A^ToMkK9[]XQgHX4THmJ]D1a4_K8J;10V_3Ua45onZG=5d^IVgW5iAhBACdWL;LD]GZQAa42GcPY[URpH:;AF[72dWBD2?D765i8KAO^K1cECdM6JeH?nI4JPD1jLbO0V^KN^kWeU0XVB:gdK0VLDgR=?fK92jN7kYAU";
		System.out.println("=========================== mike cole"); test(L, cipher); 
		cipher = "kpP+tVH7Vz(;btYtWMJGLX.UtN%:JM/6Z3)Y!b5#TFB!_tVH#K%S8fCd7Et98>AR1QY2fQN1lLdEDW63X5Tp.GJlP)-Ctf1zdUOV_/4JZ(K;)Y+pl5TB/EXtKLESHV6!B!PptY%^3M(V&zk%RbF_H>CX93lW#LN+t<RLD:dCq.;bz1AH#GcU57f_DpR4HD-MZ/W6V!QN3_lGcZ;7)XtJM&EX%:U-<RdQYP+19S5C:tdGftJ7Mb;)NY(WE1Y3Z9YF8(./K&Tp)1S>Bdl+ttRttG;CK;#V)%:JZ(M88k;NY5W:FEZFCX(1q+PtUj#-V>XZJM^Ftt+3HMXFASXpK^+1";
		System.out.println("=========================== mike eaton"); test(L, cipher); 
		cipher = "jfHQCHbc[GS`DlnMz[HKgaPH:VIbA2bG@Tz[HReWQU>H5k6GzY8QCH0HRi[BechjH^LmH9nS;]aHl=3KZHRDMHIJA1<:E@f4U]Y8F2g0cNmSVaXdbT<BOWQI;J>5i^HHA46kjGzh[9RLCH@=nlKGQM`5W1H3]8ifH0d^RkJ:Q2HHjmDW`UJlL0H^g`@4i6XeBGI5=Q:[fL3HKN;]YG^bMd=Q17?Ie8VQTmQ]Hcb[a9hSJSzb1i>Q1N?CmQ:E3<2GHR5@[F9XRz:7dcVWdIUe84RBNd<RkD]0e8<RKE09NOQl^[O=;4MnSTd1W7fzHChm9?R3jHSU1naA_3gBQBV0";
		System.out.println("=========================== tony baloney 1"); test(L, cipher); 
		cipher = ",CurN[?dISMT^yhvosdWSPFYwptc+gbfdBzOR`yA+0ZIG-VYa6+_C-UroLZD60Zfu4Jbsdq4{pikGFBJtTc:zS+:OS]mNjxlIJJWgu9vP[C,4Yy7UXr`hJsFclXfN@wtVRKTa8-NB_oDxZrZsUdliqXj4dIWn@JSdl9p,XT`kTU-UlFa[-zX@JhkYq+b@26gGRdfOyDpcA+Vi20ZBu,dE_oZbT^vjIGtT`C2ULZn*YW_PzufSU9rUo[sbZFTUWhw6BGRI_taD-qVrNisp+nmoWxFVjVtu9T`OE_,1n^oW[gYJrTy{CsHhaL]?cR_o-V+ZlDibTUdXjq-SgFZfzZl";
		System.out.println("=========================== tony baloney 2"); test(L, cipher); 
		cipher = Ciphers.cipher[0].cipher;
		cipher = Periods.rewrite3(cipher, 19);
		System.out.println("=========================== z340 period 19 scheme"); test(L, cipher); 
		cipher = Ciphers.cipher[5].cipher;
		cipher = Periods.rewrite3(cipher, 15);
		System.out.println("=========================== z340 flipped, period 15 scheme"); test(L, cipher);
		*/
		cipher = "Cz^J+Op7<FBy-++)W5tE|DYBpbTMKOU+R/NpkSzZO8A|K;+2<cH>V^VPk|1LTG2d49CE(#O%DWY.<*Kf)Np+BcM+^7zlUZ8tj*GWVd|3()p5FOL#+P++zHR&4KJB2kSy:pp/_9M+FlO-*dCkF>2D(p8R^q%;2UcXGV.zL|#5+Kfj#O+_NYz+@L9(G2Jb+ZR2FBcyA64Kd<M+V+WcPOSHT/()p-zlUW<7tB_YOB*-Cc|FkdRJ|*5T4M.+&BF>MDly#+N|5FBc(;8Rz69S^f524b.cV4t++lGFN*:HER>plUZ5-+yBX1zBK(Op^.fMqG2|c.3L16C<+FlWB|)LRcT+";
		System.out.println("=========================== experiment "); test(L, cipher);
		
	}
	
	public static void split() {
		for (int n=1; n<20; n++) {
			for (int i=0; i<n; i++) {
				System.out.println(n + " " + i*340/n + " " + ((i+1)*340/n-1));
			}
		}
	}

	/** http://zodiackillersite.com/viewtopic.php?p=47926#p47926 */
	public static void testRegions() {
		String cipher = null;
		
		int n = 170; // region length
		cipher = Ciphers.cipher[1].cipher;
		System.out.println("z408");
		for (int i=0; i<cipher.length()-n+1; i++) {
			String sub = cipher.substring(i, i+n);
			double pcs2 = perfectCycleScoreFor(2, sub, 3);
			double pcs3 = perfectCycleScoreFor(3, sub, 3);
			double pcs4 = perfectCycleScoreFor(4, sub, 3);
			System.out.println(i + "	" + pcs2 + "	" + pcs3 + "	" + pcs4);
		}
		
		cipher = Ciphers.cipher[0].cipher;
		System.out.println("z340");
		for (int i=0; i<cipher.length()-n+1; i++) {
			String sub = cipher.substring(i, i+n);
			double pcs2 = perfectCycleScoreFor(2, sub, 3);
			double pcs3 = perfectCycleScoreFor(3, sub, 3);
			double pcs4 = perfectCycleScoreFor(4, sub, 3);
			System.out.println(i + "	" + pcs2 + "	" + pcs3 + "	" + pcs4);
		}
	}
	
	/** http://zodiackillersite.com/viewtopic.php?p=47927#p47927 */
	public static void testSplits() {
		String cipher = null;
		cipher = Ciphers.cipher[1].cipher;
		System.out.println("z408");
		for (int i=1; i<cipher.length()-1; i++) {
			String left = cipher.substring(0, i);
			String right = cipher.substring(i);
			if (left.length() + right.length() != cipher.length()) throw new RuntimeException("Bad substrings");
			double pcs2l = perfectCycleScoreFor(2, left, 3);
			double pcs2r = perfectCycleScoreFor(2, right, 3);
			double pcs3l = perfectCycleScoreFor(3, left, 3);
			double pcs3r = perfectCycleScoreFor(3, right, 3);
			double pcs4l = perfectCycleScoreFor(4, left, 3);
			double pcs4r = perfectCycleScoreFor(4, right, 3);
			System.out.println(i + "	" + pcs2l + "	" + 
					pcs2r + "	" + 
					pcs3l + "	" + 
					pcs3r + "	" + 
					pcs4l + "	" + 
					pcs4r + "	"); 
		}
		
		cipher = Ciphers.cipher[0].cipher;
		System.out.println("z340");
		for (int i=1; i<cipher.length()-1; i++) {
			String left = cipher.substring(0, i);
			String right = cipher.substring(i);
			if (left.length() + right.length() != cipher.length()) throw new RuntimeException("Bad substrings");
			double pcs2l = perfectCycleScoreFor(2, left, 3);
			double pcs2r = perfectCycleScoreFor(2, right, 3);
			double pcs3l = perfectCycleScoreFor(3, left, 3);
			double pcs3r = perfectCycleScoreFor(3, right, 3);
			double pcs4l = perfectCycleScoreFor(4, left, 3);
			double pcs4r = perfectCycleScoreFor(4, right, 3);
			System.out.println(i + "	" + pcs2l + "	" + 
					pcs2r + "	" + 
					pcs3l + "	" + 
					pcs3r + "	" + 
					pcs4l + "	" + 
					pcs4r + "	"); 
		}
		
	}
	
	public static void testChunks(String cipher, int L) {
		for (int i=0; i<cipher.length()-1; i++) {
			for (int j=i+1; j<cipher.length(); j++) {
				String chunk = cipher.substring(i, j);
				double pcs2 = perfectCycleScoreFor(2, chunk, 3);	
				System.out.println(pcs2 + " " + chunk.length() + " " + i + " " + (j-1));
			}
		}
		
	}

	public static void main(String[] args) {
		 //testShuffles();
		 //testVariousCiphers();
		test(4, Ciphers.cipher[0].cipher);
		test(4, "CzWJ+Op7<FBy-++)W5tE|DYBpbTMKOU+R/NpkSzZO8A|K;+2<cHVl^VPk|1LTG2d9CE>(#O%DWY.<*Kf)Np+:BcM^+lU8Z*GVW3(p)L#zHJByO++RK2Spp7ztjd|5FP+&4k/_9M+FlO-*dCkF>2D(p8R^q%;2UcXGV.zL|#5+Kfj#O+_NYz+@L9(G2Jb+ZR2FBcyA64Kd<M+V+^cPOSHT/()p-zlUW<7tB_YOB*-Cc|FkdRJ|*5T4M.+&BF>MDly#+N|5FBc(;8Rz69S^f524b.cV4t++lGFN*:4HER>pUZ5-+yBX1zBK(Op^.fMqG2|c.3L16C<+FlWB|)LRcT+");
		//testRegions();
		//testSplits();
		//testChunks(Ciphers.cipher[0].cipher, 2);
		/*System.out.println(perfectCycleScoreFor(2, Ciphers.cipher[0].cipher, 3));
		System.out.println(perfectCycleScoreFor(2, Ciphers.cipher[1].cipher, 3));
		System.out.println(perfectCycleScoreFor(3, Ciphers.cipher[0].cipher, 3));
		System.out.println(perfectCycleScoreFor(3, Ciphers.cipher[1].cipher, 3));
		System.out.println(perfectCycleScoreFor(4, Ciphers.cipher[0].cipher, 3));
		System.out.println(perfectCycleScoreFor(4, Ciphers.cipher[1].cipher, 3));*/
		//testZ408Plaintext(5);
		//split();
		//test(4, "ABCDEFGHIJKLMNOPQRSTUFVEWXVYZabcdefghXijklmnopqrsPtuJJJUEvWwxkBpJXXaplRcyzRM01Of0XSspL2KCQNDMZJDgo2bJKILTTJ3LGzJMy4qW5i6FRmfdh7XN8QMoAup8yrcMavPJfdJ0wVxWszSFJMtRL8MXGrZXnNpRefgPXJAJUb1JCBHJKdaqcki3liMuJJEGfYRUWakrqORwlEsJ3SDNS51ugwxkEzgF6XOV7JtRJWEIkEBli2JGm8UpXhYWPXkcWJRflH0ZRWvvlsQ4MXqAJkl0zCMJaGPYJzukD6MXWJ0Czk9X8Cxl7ZtFLzRcJEXG1Oa6PMU");
		//test(3, "ABCCBACBA");
		//test(2, Ciphers.cipher[6].cipher);
		//test(2, "dEB+*5k.L(MVE5FV5B<MF<Sf9pl/C|DPYL2c+ztZ2H+M8|CV@K<R/9^%OF7TB29^4OFTUt*5cZG|TC7zG)pcl-+MVW)+k_Rq#2pb&RddG+4dl5||.UqLcW<6N:(+H*;>^D(+4(8KSTfN:^j*Xz6-z/JNbjROp+8zF*K<SBKl%WVM)R)WkLKJy7t-cYAO&Dp+fZ+B.;+G1BCOy-RR+4>f|p+dp1*HBO|pOGp+2|5J+JYM(+pzOUNyBO+l#2E.B)+lXz6PYA>#Z3P>L#2bkN|<z2p+l2_cFKUcy^D4ct+B31c_8R(UVF5C^W(cFHk.#KSMF;+Fz9G++|TB4-y.LWBO");
		//while (true)
			//testShufflesWithAllEvensOrOddsFound(4);
		//testProcess();
		//testCountProbs(new float[] {2.4584872E-24f, 9.121544E-22f, 5.8000224E-21f, 4.9299357E-20f, 1.1834862E-19f, 2.6677867E-17f, 3.7667542E-17f});
		//testCountProbs(new float[] {3.5590823E-23f, 1.8017861E-22f, 1.509793E-21f, 2.5756405E-21f});
		
		
		/*for (int i=0; i<100; i++) {
			testShuffleUntilTargetFound(4.519328E-17f);
		}*/
		//testShuffleStats(2, Ciphers.cipher[0].cipher, 10000);

	}


}
