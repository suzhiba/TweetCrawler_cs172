import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
//import java.io.Reader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.simple.parser.ParseException;


class WebDocument {
	public String user;
	public String tweet;
	public String time;
	public String url;
	public String title;
	public String geo;
	
	public WebDocument(String usr, String twt, String time, String u, String title, String g) {
		this.user=usr;
		this.tweet=twt;
		this.time=time;
		this.url = u;
		this.title = title;
		this.geo = g;
	}
}

public class LuceneSearch{
	public static final int MAX_FILE_COUNT = 1;
	public static final String INDEX_DIR = "Index";
	public static void main(String[] args) throws CorruptIndexException, IOException, ParseException {
		//get input from file and make page entries
		//index all pages
		//search per user query request
		getFile(); // read from file and index it
		search("coffee",5);
	}
	
	public static void getFile () throws FileNotFoundException, ParseException{
		for(int i = 1; i <= MAX_FILE_COUNT; i++){
			try{
				File file = new File("D:\\Java Workspace\\LuceneSearch\\file" + i + ".txt");
				FileReader fileReader = new FileReader(file);
				BufferedReader buff = new BufferedReader(fileReader);
//				StringBuffer strBuff = new StringBuffer();
				String line ="";
				while ((line = buff.readLine() ) != null){
					String User = line.substring(line.indexOf("User")+7, line.indexOf("Time")-3);
					String Time = line.substring(line.indexOf("Time")+6, line.indexOf("Tweet")-2);
					String Tweet = line.substring(line.indexOf("Tweet")+8, line.indexOf("LinkUrl")-3);
					String LinkUrl = line.substring(line.indexOf("LinkUrl")+9, line.indexOf("LinkTitle")-2);
					String LinkTitle = line.substring(line.indexOf("LinkTitle")+11, line.indexOf("Geo")-2);
					String Geo = line.substring(line.indexOf("Geo")+5, line.length()-1);
					WebDocument page = new WebDocument(User,Tweet,Time,LinkUrl,LinkTitle,Geo);
					index(page);
//					System.out.println(page.user);
				}

				fileReader.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void index (WebDocument page) {
		File index = new File(INDEX_DIR);	
		IndexWriter writer = null;
		try {	
			IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_34, new StandardAnalyzer(Version.LUCENE_34));
			writer = new IndexWriter(FSDirectory.open(index), indexConfig);
//			System.out.println("Indexing to directory '" + index + "'...");	
			Document luceneDoc = new Document();
			//get fields from page and insert into document
			luceneDoc.add(new Field("user", page.user, Field.Store.YES, Field.Index.NOT_ANALYZED,Field.TermVector.YES));
			luceneDoc.add(new Field("tweet", page.tweet, Field.Store.YES, Field.Index.ANALYZED,Field.TermVector.YES));
			luceneDoc.add(new Field("time", page.time, Field.Store.YES, Field.Index.ANALYZED,Field.TermVector.YES));
			luceneDoc.add(new Field("url", page.url, Field.Store.YES, Field.Index.NOT_ANALYZED,Field.TermVector.YES));
			luceneDoc.add(new Field("title", page.title, Field.Store.YES, Field.Index.ANALYZED,Field.TermVector.YES));
			luceneDoc.add(new Field("geo", page.geo, Field.Store.YES, Field.Index.ANALYZED,Field.TermVector.YES));
			//write into lucene
//			System.out.println(luceneDoc.get("user"));
			writer.addDocument(luceneDoc);			
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (writer !=null)
				try {
					writer.close();
				} catch (CorruptIndexException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
	
	public static TopDocs search (String queryString, int topk) throws CorruptIndexException, IOException {
		
		IndexReader indexReader = IndexReader.open(FSDirectory.open(new File(INDEX_DIR)), true);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		
		QueryParser queryparser = new QueryParser(Version.LUCENE_34, "tweet", new StandardAnalyzer(Version.LUCENE_34));

		try {
			StringTokenizer strtok = new StringTokenizer(queryString, " ~`!#$%^&*()_-+={[}]|;'<>,./?\"\'\\/\n\t\b\f\r");
			String querytoparse = "";
			while(strtok.hasMoreElements()) {
				String token = strtok.nextToken();
				querytoparse += "user:" + token + "^1" + "tweet:" + token+ "^1.5";
				querytoparse += token + "\n";
			}		
			Query query = queryparser.parse(querytoparse); 
		    System.out.println(query.toString());
			
		    TopDocs results = indexSearcher.search(query, topk);
			System.out.println("Total Hits: " + results.totalHits);
			System.out.println("Max Score: " + results.getMaxScore() + "\n");
			
			ScoreDoc[] hits = results.scoreDocs;
			for(int h = 0; h < topk; ++h){
//				Field fieldEnum = .fields();
//				while(fieldEnum.hasMoreElements()){
//					Field field = (Field) fieldEnum.nextElement();
//					String val = doc.get(field.name());
//					System.out.println(field.name() + ": " + val);
//				}
				
				System.out.println(hits[h].toString()); //doc id and score of each hit
				//get tweet content
				Document d = indexSearcher.doc(hits[h].doc);
				System.out.println("Tweet: " + d.getFieldable("tweet"));
//				TermFreqVector t = indexReader.getTermFreqVector(hits[h].doc,"tweet");
//				System.out.println(t);
			}
			return results;			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			indexSearcher.close();
			indexReader.close();
		}
		return null;
	}
	
}