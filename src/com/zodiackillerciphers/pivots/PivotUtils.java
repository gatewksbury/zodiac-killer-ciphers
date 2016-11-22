package com.zodiackillerciphers.pivots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zodiackillerciphers.ciphers.Ciphers;
import com.zodiackillerciphers.ciphers.generator.CandidatePlaintext;
import com.zodiackillerciphers.ciphers.generator.TrigramUtils;

public class PivotUtils {
	
	public static boolean DEBUG = false;

	/** suitable defaults */
	public static List<Pivot> findPivots(String cipher, int minsize) {
		String[] grid = Ciphers.grid(cipher, Pivot.W, true);
		List<Direction> list = new ArrayList<Direction>();
		list.add(Direction.N);
		list.add(Direction.S);
		list.add(Direction.E);
		list.add(Direction.W);
		List<Pivot> pivots = findPivots(grid, minsize, list, false);
		pivots = removeSubstrings(pivots);
		//System.out.println(pivots);
		return pivots;
	}
	/**
	 * find all pivots in the given grid of strings. search the given
	 * directions.
	 */
	public static List<Pivot> findPivots(String[] grid, int minsize,
			List<Direction> searchDirections, boolean allowWrapping) {
		if (grid == null)
			return null;
		if (minsize < 2)
			return null;
		if (searchDirections == null)
			return null;

		/*
		 * Consider a position in the grid. We want to find a pivot there. To
		 * look, we consider each pair of directions from the allowed search
		 * directions.
		 */

		List<Pivot> list = new ArrayList<Pivot>();

		for (int row = 0; row < grid.length; row++) {
			for (int col = 0; col < grid[row].length(); col++) {
				for (int i = 0; i < searchDirections.size() - 1; i++) {
					for (int j = i + 1; j < searchDirections.size(); j++) {
						findPivot(list, grid, row, col, minsize,
								searchDirections.get(i),
								searchDirections.get(j), allowWrapping);
					}
				}

			}
		}
		return list;
	}
	
	
	public static void findPivot(List<Pivot> searchResults, String[] grid, int row, int col, int minsize, Direction d1, Direction d2, boolean allowWrapping) {
		findPivot(searchResults, grid, row, col, row, col, row, col, minsize, d1, null, d2, null, new ArrayList<Integer>(), "", allowWrapping);
	}

	/** convert row/col to position */
	static int pos(String[] grid, int row, int col) {
		return row*grid[0].length() + col;
	}
	
	static void debug(String msg) {
		if (DEBUG) System.out.println(msg);
	}
	
	/** look for candidate pivots in the given grid at the given position */
	static void findPivot(List<Pivot> searchResults, String[] grid, int row, int col, int r1, int c1, int r2, int c2, int minsize,
			Direction direction1, DirectionDelta d1, Direction direction2, DirectionDelta d2, List<Integer> seen,
			String ngram, boolean allowWrapping) {
		
		int H = grid.length;
		int W = grid[0].length();
		
		/* true if search has just started at (row,col) */
		//boolean starting = row == r1 && col == c2 && row == r2 && col == c2 && !seen.contains(pos(grid, row, col));
		boolean starting = d1 == null && d2 == null; 

		// if we are just starting, don't bother checking exit conditions, just start recursing

		int pos1 = pos(grid, r1, c1);
		int pos2 = pos(grid, r2, c2);
		
		if (allowWrapping) {
			// if we allow for wrapping then fix any out of bounds positions
			//System.out.println("before " + r1 + " " + c1 + " " + r2 + " " + c2 + " " + pos1 + " " + pos2);
			if (r1 < 0) r1 = H-1;
			else if (r1 > H-1) r1 = 0;
			if (c1 < 0) c1 = W-1;
			else if (c1 > W-1) c1 = 0;
			if (r2 < 0) r2 = H-1;
			else if (r2 > H-1) r2 = 0;
			if (c2 < 0) c2 = W-1;
			else if (c2 > W-1) c2 = 0;
			// recompute positions to account for wrapping
			pos1 = pos(grid, r1, c1);
			pos2 = pos(grid, r2, c2);
			//System.out.println("after " + r1 + " " + c1 + " " + r2 + " " + c2 + " " + pos1 + " " + pos2);
		}
		debug(row + " " + col + " " + r1 + " " + c1 + " " + r2 + " " + c2 + " pos1 " + pos1 + " pos2 " + pos2 + " ms " + minsize + " dir1 " + direction1 + " delta1 " + d1 + " dir2 " + direction2 + " delta2 " + d2 + " ngram " + ngram + " seen " + seen);
		
		if (!starting) {
			// check exit conditions
			// if no exit condition is met, continue recursive search
			if (TrigramUtils.ignore(ngram)) {
				debug("exit: invalid symbols in ngram");
				return;
			}
			boolean oob = false;
			if (!allowWrapping && oob(grid, r1, c1) || oob(grid, r2, c2)) {// exit condition: out of bounds
				debug("exit: out of bounds");
				return; 
			}
			else if (r1 == r2 && c1 == c2) { // exit condition: positions are equal
				debug("exit: equal pos");
				return;
			}
			else if (r1 == row && c1 == col) { // exit condition: returned to starting position
				debug("exit: back to start");
				return;
			}
			else if (seen.contains(pos1) || seen.contains(pos2)) {
				debug("exit: already seen " + pos1 + " " + seen.contains(pos1) + " " + pos2 + " " + seen.contains(pos2));
				return;
				// exit condition: seen this position before 
			}
			else if (!oob) {
				char ch1 = grid[r1].charAt(c1); 
				char ch2 = grid[r2].charAt(c2);
				if (ch1 != ch2) {
					debug("exit: " + ch1 + " != " + ch2);
					return; // exit condition: symbols do not match
				}
			}
		}
		
		
		// if we got here, exit conditions were not met.
		
		// for extending the ngram 
		char ch = grid[r1].charAt(c1);
		ngram += ch;
		// Do we already have a pivot (or qualifying pivot in progress?)
		if (ngram.length() >= minsize) {
			Pivot found = new Pivot();
			found.ngram = ngram;
			found.positions = new ArrayList<Integer>(seen);
			found.positions.add(pos1);
			found.positions.add(pos2);
			found.directions = new Direction[] {direction1, direction2};
			//System.out.println(ngram.length() + " Found " + found);
			searchResults.add(found);
		}
		

		// continue the search by recursing into neighboring positions
		
		// track two positions (p1 and p2) 
		// each position is associated with a Direction (d1 and d2)
		// two types of direction: simple cardinal direction (simple), or "ANY" which is a unrestricted search of 
		// contiguous positions (any)
		// if direction is simple, search only considers one direction
		// if direction is any, search has to consider all directions
		
		List<DirectionDelta> deltas1 = Direction.toDelta(direction1);
		List<DirectionDelta> deltas2 = Direction.toDelta(direction2);

		
		// we have to consider all possible combinations of deltas.
		for (DirectionDelta delta1 : deltas1) {
			for (DirectionDelta delta2 : deltas2) {
				// mark current positions as seen.  unmark them when we finish recursing.
				seen.add(pos1);
				if (pos2 != pos1) seen.add(pos2);
				findPivot(searchResults, grid, row, col, r1+delta1.drow, c1+delta1.dcol, r2+delta2.drow, c2+delta2.dcol, minsize, direction1, delta1, direction2, delta2, seen, ngram, allowWrapping);
				// "unsee" positions
				seen.remove(seen.size()-1);
				if (pos2 != pos1) seen.remove(seen.size()-1);
			}
		}
		
	}
	
	/** return all distinct pairs of compatible pivots */
	public static List<PivotPair> pairsFrom(List<Pivot> list) {
		if (list == null || list.size() < 2) return null;
		List<PivotPair> result = new ArrayList<PivotPair>();
		for (int i=0; i<list.size()-1; i++) {
			for (int j=i+1; j<list.size(); j++) {
				Pivot p1 = list.get(i);
				Pivot p2 = list.get(j);
				if (compatible(p1, p2)) result.add(new PivotPair(new Pivot[] {p1, p2}));
			}
			
		}
		return result;
	}
	
	static boolean oob(String[] grid, int row, int col) {
		return row < 0 || row > grid.length - 1 || col < 0
				|| col > grid[row].length() - 1;
	}
	
	/** returns true if the two pivots are oriented in the same directions, and have legs that form right angles,
	 * and do not intersect */
	public static boolean compatible(Pivot pivot1, Pivot pivot2) {
		
		Set<Integer> seen = new HashSet<Integer>();
		for (Integer i : pivot1.positions) {
			if (seen.contains(i)) return false;
			seen.add(i);
		}
		for (Integer i : pivot2.positions) {
			if (seen.contains(i)) return false;
			seen.add(i);
		}
		
		Arrays.sort(pivot1.directions);
		Arrays.sort(pivot2.directions);
		
		String d1 = pivot1.dumpDirections();
		String d2 = pivot2.dumpDirections();
		
		Direction dir1 = pivot1.directions[0];
		Direction dir2 = pivot1.directions[1];
		
		//System.out.println(d1 + ", " + d2 + ", " + dir1 + ", " + dir2 + ", " + Direction.perpendicular(dir1, dir2));
		return d1.equals(d2) && Direction.perpendicular(dir1, dir2);
	}
	/** return list of pivots from the given pivot pairs */
	public static List<Pivot> toList(List<PivotPair> pairs) {
		if (pairs == null) return null;
		List<Pivot> list = new ArrayList<Pivot>();
		for (PivotPair pair : pairs) {
			list.add(pair.pivots[0]);
			list.add(pair.pivots[1]);
		}
		return list;
	}
	static void test1() {
		String cipher = Ciphers.cipher[0].cipher;
		String[] grid = Ciphers.grid(cipher, 17);
		List<Direction> list = new ArrayList<Direction>();
		list.add(Direction.N);
		list.add(Direction.S);
		list.add(Direction.E);
		list.add(Direction.W);
		//list.add(Direction.NW);
		//list.add(Direction.SW);
		//list.add(Direction.NE);
		//list.add(Direction.SE);
		//list.add(Direction.ANY);
		//list.add(Direction.ANY);
		List<Pivot> pivots = findPivots(grid, 4, list, false);
		//for (Pivot p : pivots) System.out.println(p);
		
		List<PivotPair> pairs = pairsFrom(pivots);
		for (PivotPair pair : pairs) System.out.println("pair " + pair);
		
		
	}
	
	static void test2() {
		List<Direction> list = new ArrayList<Direction>();
		list.add(Direction.N);
		list.add(Direction.S);
		list.add(Direction.W);
		list.add(Direction.E);
		list.add(Direction.NE);
		list.add(Direction.SE);
		list.add(Direction.NW);
		list.add(Direction.SW);
		list.add(Direction.ANY);
		
		Collections.sort(list);
		System.out.println(list);
	}

	/** return a new list of pivots with the substring (redundant) pivots removed */
	public static List<Pivot> removeSubstrings(List<Pivot> pivots) {
		if (pivots == null) return null;
		Set<Integer> indexes = new HashSet<Integer>();
		for (int i=0; i<pivots.size()-1; i++) {
			for (int j=i+1; j<pivots.size(); j++) {
				Pivot p1 = pivots.get(i);
				Pivot p2 = pivots.get(j);
				if (p1.substringOf(p2)) {
					//System.out.println(p1 + " substring of " + p2);
					indexes.add(i);
				} else if (p2.substringOf(p1)) {
					//System.out.println(p2 + " substring of " + p1);
					indexes.add(j);
				} 
			}
		} 
		
		List<Pivot> result = new ArrayList<Pivot>();
		for (int i=0; i<pivots.size(); i++) {
			if (indexes.contains(i)) continue;
			result.add(pivots.get(i));
		}
		return result;
	}
	
	/**
	 * If the cipher has a pair of pivots, the pivot score gets the frequency of
	 * each symbol involved in the pivots, and multiplies them all together. A
	 * lower score suggests a lower probability of the pivots occurring by random
	 * chance. A higher score suggests a greater probability of the pivots
	 * occurring by random chance.
	 * 
	 * If there is more than one pivot, pick the one with the smallest score
	 */
	public static long pivotScore(String cipher, Map<Character, Integer> countMap) {
		return pivotScore(cipher, countMap, false);
	}
	public static long pivotScore(String cipher, Map<Character, Integer> countMap, boolean showSteps) {
		CandidatePlaintext cp = new CandidatePlaintext(0, cipher);
		boolean pivots = cp.criteriaHasPivots();
		long pivotScore = 0;
		if (pivots) {
			pivotScore =  cp.pivotScore(countMap, showSteps);
		}
		return pivotScore;
	}
	
	public static void main(String[] args) {
		//test1();
		String cipher = "TzZ399:k9f&>2d*_y@O2HT@4EPZB+#)NR(X</V%PKG&@czRq_/V>tECM+N^<zS_p8F*_SBOb)TJtE&C#qPKPA|VJF7zyKGWHFfSOG12H.KCOJ+J+2yFGU_>YE_5Jc-(EyPEpkNf_p+XAAyK/9DCN::N<3UtWZJ6/Z@PMTZ&UWL-CVF.#pbbbpE#bV5-J8M2U/V%8K8D>t3:2ARL^lJ.1Oly*B+M^OV1;#%CEK_7%UOTdSG2dCMfWTZPUWLXC9Kz;;GXK8|2YWlL*-Kcb4+lRK-*8D%cG|KPb.+VyJb&PkWG6FBc|2clLB|*P|5F(bK+b.cVRJ|*KP|5FBc|HERGM";
		System.out.println(PivotUtils.pivotScore(cipher, Ciphers.countMap(cipher), true));
	}

}