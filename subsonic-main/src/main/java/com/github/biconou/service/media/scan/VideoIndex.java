package com.github.biconou.service.media.scan;

import com.github.biconou.subsonic.service.MediaScannerService;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.TranscodingService;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by remi on 07/04/2016.
 */
public class VideoIndex {

    private static final org.slf4j.Logger LOG_SCAN = LoggerFactory.getLogger(MediaScannerService.class);

    private static final String lock = "lock";
    private IndexWriter indexWriter = null;
    private IndexReader indexReader = null;
    private static final String LUCENE_DIR = "lucene2";

    private static final String FIELD_ID = "id";


    private SettingsService settingsService = null;
    private TranscodingService transcodingService = null;


    private File getIndexRootDirectory() {
        return new File(SettingsService.getSubsonicHome(), LUCENE_DIR);
    }

    private File getIndexDirectory() {
        return new File(getIndexRootDirectory(), "video");
    }

    /**
     *
     * @return
     * @throws IOException
     */
    /*
    private IndexWriter createIndexWriter() throws IOException {
        File dir = getIndexDirectory();
        try {
            indexWriter = new IndexWriter(FSDirectory.open(dir), new SimpleAnalyzer(), false, IndexWriter.MaxFieldLength.UNLIMITED);
        } catch (IOException e) {
            indexWriter = new IndexWriter(FSDirectory.open(dir), new SimpleAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);
        }

        return indexWriter;
    }
    */

    /**
     *
     * @return
     */
    /*
    public IndexWriter getIndexWriter() {
        if (indexWriter == null) {
            synchronized (lock) {
                if (indexWriter == null) {
                    try {
                        LOG_SCAN.debug("Create video index writer.");
                        indexWriter = createIndexWriter();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return indexWriter;
    }*/

    /**
     *
     * @return
     */
    /*
    public IndexReader getIndexReader() {
        if (indexReader == null) {
            synchronized (lock) {
                if (indexReader == null) {
                    try {
                        LOG_SCAN.debug("Create video index reader.");
                        indexReader = createIndexReader();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return indexReader;
    }*/

    /*
    private IndexReader createIndexReader() throws IOException {
        File dir = getIndexDirectory();
        indexReader = IndexReader.open(FSDirectory.open(dir), false);
        return indexReader;
    }*/

    /**
     *
     * @param mediaFile
     * @return
     * @throws IOException
     */
    /*
    public int getDocumentNum(MediaFile mediaFile) throws IOException {
        Searcher searcher = new IndexSearcher(getIndexReader());
        TermQuery query = new TermQuery(new Term(FIELD_ID, "" + mediaFile.getId()));
        TopDocs hits = searcher.search(query,1);
        if (hits != null && hits.scoreDocs != null && hits.scoreDocs.length > 0) {
            return hits.scoreDocs[0].doc;
        } else  {
            return -1;
        }
    }*/

    /**
     *
     * @param mediaFile
     * @return
     * @throws IOException
     */
    /*
    public Document getDocument(MediaFile mediaFile) throws IOException {
        int existingDocNum = getDocumentNum(mediaFile);
        if (existingDocNum == -1) {
            return  null;
        } else {
            return indexReader.document(existingDocNum);
        }
    }*/

    /**
     *
     * @param mediaFile
     * @throws IOException
     */
    /*
    public void createOrReplace(MediaFile mediaFile) throws IOException, InterruptedException {

        LOG_SCAN.debug("createOrReplace "+mediaFile.getPath());

        int existingDocNum = getDocumentNum(mediaFile);
        if (existingDocNum != -1) {
            LOG_SCAN.debug("document "+mediaFile.getPath()+" exists in index. Delete it.");
            getIndexReader().deleteDocument(existingDocNum);
        } else {
            LOG_SCAN.debug("document "+mediaFile.getPath()+" does not exists in index.");
        }

        LOG_SCAN.debug("Create thumbnails using ffmpeg for "+mediaFile.getPath());

        File transcodingDir = getTranscodingService().getTranscodeDirectory();
        String ffmpeg = transcodingDir.getAbsolutePath() + "/ffmpeg.exe";
        String videoFileName = mediaFile.getPath();

        ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-i", videoFileName, "-vf", "fps=1/60", "img%03d.jpg");
        File tempDir = Files.createTempDir().getAbsoluteFile();
        pb.directory(tempDir);
        File log = new File(tempDir.getAbsolutePath() + "/videoThumbs.log");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        Process p = pb.start();
        p.waitFor();
        if (p.exitValue() != 0) {
            throw new RuntimeException("Error in process that creates video thumbnails");
        }

        LOG_SCAN.debug("Create new index document for "+mediaFile.getPath());

        Document doc = new Document();
        doc.add(new NumericField(FIELD_ID, Field.Store.YES, false).setIntValue(mediaFile.getId()));


        for (File file : tempDir.listFiles()) {
            if (file.getName().contains("img")) {
                doc.add(new Field(file.getName(), Files.toByteArray(file), Field.Store.YES));
            }
            file.delete();
        }

        LOG_SCAN.debug("Add document to index for "+mediaFile.getPath());

        getIndexWriter().addDocument(doc);
        getIndexWriter().commit();

        LOG_SCAN.debug("Delete temporary dir "+tempDir.getAbsolutePath());

        tempDir.delete();
    }*/

    /**
     *
     * @param settingsService
     */
    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     *
     * @return
     */
    public SettingsService getSettingsService() {
        return settingsService;
    }

    /**
     *
     * @return
     */
    public TranscodingService getTranscodingService() {
        return transcodingService;
    }

    /**
     *
     * @param transcodingService
     */
    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

}
