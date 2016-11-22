package com.zodiackillerciphers.lucene;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.document.Document;


public class AKWilks {
	static String[] choices = new String[] {
		"oeukaqg",
		"eukaqgw",
		"gwmcsiy",
		"dtjzpfv",
		"csiyoeu",
		"rhxndtj",
		"wmcsiyo",
		"oeukaqg",
		"mcsiyoe"
	};
	
	static int L = 7;
	
	static Set<String> found = new HashSet<String>();
	public static void bruteWords() {
		int count = 0;
		for (int a=0; a<L; a++) {
			for (int b=0; b<L; b++) {
				for (int c=0; c<L; c++) {
					for (int d=0; d<L; d++) {
						for (int e=0; e<L; e++) {
							for (int f=0; f<L; f++) {
								for (int g=0; g<L; g++) {
									for (int h=0; h<L; h++) {
										for (int i=0; i<L; i++) {
											String word = "" + 
												choices[0].charAt(a) + 
												choices[1].charAt(b) + 
												choices[2].charAt(c) + 
												choices[3].charAt(d) + 
												choices[4].charAt(e) + 
												choices[5].charAt(f) + 
												choices[6].charAt(g) + 
												choices[7].charAt(h) + 
												choices[8].charAt(i);
											
											String query = "";
											for (int len=6; len<=9; len++) {
												for (int pos=0; pos<=word.length()-len; pos++) {
													query += "word:" + word.substring(pos, pos+len) + " ";
												}
											}
											word(query, count);
											
											count++;
											if (count % 100000 == 0) System.out.println(count+"...");
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	public static void word(String query, int count) {
		//System.out.println(query);
		Results r = LuceneService.query(query);
		if (r.docs == null) return;
		for (Document doc : r.docs) {
			String word = LuceneService.wordFrom(doc);
			if (!found.contains(word)) {
				System.out.println(word + " at iteration " + count);
				found.add(word);
			}
		}
	}
	
	/** http://www.zodiackillersite.com/viewtopic.php?p=47602#p47602 */
	public static String shiftedLettersFor(char ch) {
		int[] shifts = new int[] {-9, -6, -3, 0, 3, 6, 9};
		String s = "" + ch;
		s = s.toUpperCase();
		int a = s.charAt(0) - 65;
		String result = "";
		
		for (int shift : shifts) {
			int a2 = a + shift;
			if (a2 < 0) a2 += 26;
			a2 %= 26;		
			result += "" + (char) (a2+65);
		}
		return result;
	}
	
	public static void main(String[] args) {
		//bruteWords();
		
		for (char ch = 65; ch <65+26; ch++) {
			for (char c = 65; c<65+26; c++) {
				String s = shiftedLettersFor(c);
				if (s.contains(""+ch)) {
					System.out.println(ch + " " + s);
				}
			}
		}
		
	}
}