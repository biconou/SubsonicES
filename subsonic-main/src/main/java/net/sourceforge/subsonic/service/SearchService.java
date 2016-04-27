/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.service;

import com.github.biconou.dao.ElasticSearchClient;
import com.sun.media.jfxmedia.Media;
import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.dao.AlbumDao;
import net.sourceforge.subsonic.dao.ArtistDao;
import net.sourceforge.subsonic.domain.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.util.Version;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.sourceforge.subsonic.service.SearchService.IndexType.*;

/**
 * Performs Lucene-based searching and indexing.
 *
 * @author Sindre Mehus
 * @version $Id: SearchService.java 4385 2015-02-11 21:53:35Z sindre_mehus $
 * @see MediaScannerService
 */
public class SearchService {

    private static final Logger LOG = Logger.getLogger(SearchService.class);

    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_ALBUM = "album";
    private static final String FIELD_ARTIST = "artist";
    private static final String FIELD_GENRE = "genre";
    private static final String FIELD_YEAR = "year";
    private static final String FIELD_MEDIA_TYPE = "mediaType";
    private static final String FIELD_FOLDER = "folder";
    private static final String FIELD_FOLDER_ID = "folderId";

    private static final Version LUCENE_VERSION = Version.LUCENE_5_5_0;
    private static final String LUCENE_DIR = "lucene2";

    private ElasticSearchClient elasticSearchClient = null;

    private MediaFileService mediaFileService;
    private ArtistDao artistDao;
    private AlbumDao albumDao;

    private IndexWriter artistWriter;
    private IndexWriter artistId3Writer;
    private IndexWriter albumWriter;
    private IndexWriter albumId3Writer;
    private IndexWriter songWriter;

    public SearchService() {
        new RuntimeException("No more implemented").printStackTrace();
        //removeLocks();
    }

    public void startIndexing() {
        try {
            artistWriter = createIndexWriter(ARTIST);
            artistId3Writer = createIndexWriter(ARTIST_ID3);
            albumWriter = createIndexWriter(ALBUM);
            albumId3Writer = createIndexWriter(ALBUM_ID3);
            songWriter = createIndexWriter(SONG);
        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        }
    }

    public void index(MediaFile mediaFile) {
        try {
            if (mediaFile.isFile()) {
                songWriter.addDocument(SONG.createDocument(mediaFile));
            } else if (mediaFile.isAlbum()) {
                albumWriter.addDocument(ALBUM.createDocument(mediaFile));
            } else {
                artistWriter.addDocument(ARTIST.createDocument(mediaFile));
            }
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + mediaFile, x);
        }
    }

    public void index(Artist artist, MusicFolder musicFolder) {
        try {
            artistId3Writer.addDocument(ARTIST_ID3.createDocument(artist, musicFolder));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + artist, x);
        }
    }

    public void index(Album album) {
        try {
            albumId3Writer.addDocument(ALBUM_ID3.createDocument(album));
        } catch (Exception x) {
            LOG.error("Failed to create search index for " + album, x);
        }
    }

    public void stopIndexing() {
        throw new RuntimeException("No more implemented");
        /*
        try {
            artistWriter.optimize();
            artistId3Writer.optimize();
            albumWriter.optimize();
            albumId3Writer.optimize();
            songWriter.optimize();
        } catch (Exception x) {
            LOG.error("Failed to create search index.", x);
        } finally {
            FileUtil.closeQuietly(artistId3Writer);
            FileUtil.closeQuietly(artistWriter);
            FileUtil.closeQuietly(albumWriter);
            FileUtil.closeQuietly(albumId3Writer);
            FileUtil.closeQuietly(songWriter);
        }
        */
    }

    public SearchResult search(SearchCriteria criteria, List<MusicFolder> musicFolders, IndexType indexType) {
        throw new RuntimeException("No more implemented");
/*
        SearchResult result = new SearchResult();
        int offset = criteria.getOffset();
        int count = criteria.getCount();
        result.setOffset(offset);

        IndexReader reader = null;
        try {
            reader = createIndexReader(indexType);
            Searcher searcher = new IndexSearcher(reader);
            Analyzer analyzer = new SubsonicAnalyzer();

            MultiFieldQueryParser queryParser = new MultiFieldQueryParser(LUCENE_VERSION, indexType.getFields(), analyzer, indexType.getBoosts());

            BooleanQuery query = new BooleanQuery();
            query.add(queryParser.parse(analyzeQuery(criteria.getQuery())), BooleanClause.Occur.MUST);

            List<SpanTermQuery> musicFolderQueries = new ArrayList<SpanTermQuery>();
            for (MusicFolder musicFolder : musicFolders) {
                if (indexType == ALBUM_ID3 || indexType == ARTIST_ID3) {
                    musicFolderQueries.add(new SpanTermQuery(new Term(FIELD_FOLDER_ID, NumericUtils.intToPrefixCoded(musicFolder.getId()))));
                } else {
                    musicFolderQueries.add(new SpanTermQuery(new Term(FIELD_FOLDER, musicFolder.getPath().getPath())));
                }
            }
            query.add(new SpanOrQuery(musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()])), BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(query, null, offset + count);
            result.setTotalHits(topDocs.totalHits);

            int start = Math.min(offset, topDocs.totalHits);
            int end = Math.min(start + count, topDocs.totalHits);
            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(topDocs.scoreDocs[i].doc);
                switch (indexType) {
                    case SONG:
                    case ARTIST:
                    case ALBUM:
                        MediaFile mediaFile = mediaFileService.getMediaFile(Integer.valueOf(doc.get(FIELD_ID)));
                        addIfNotNull(mediaFile, result.getMediaFiles());
                        break;
                    case ARTIST_ID3:
                        Artist artist = artistDao.getArtist(Integer.valueOf(doc.get(FIELD_ID)));
                        addIfNotNull(artist, result.getArtists());
                        break;
                    case ALBUM_ID3:
                        Album album = albumDao.getAlbum(Integer.valueOf(doc.get(FIELD_ID)));
                        addIfNotNull(album, result.getAlbums());
                        break;
                    default:
                        break;
                }
            }

        } catch (Throwable x) {
            LOG.error("Failed to execute Lucene search.", x);
        } finally {
            FileUtil.closeQuietly(reader);
        }
        return result;
        */
    }

    private String analyzeQuery(String query) throws IOException {
        throw new RuntimeException("No more implemented");
/*
        StringBuilder result = new StringBuilder();
        ASCIIFoldingFilter filter = new ASCIIFoldingFilter(new StandardTokenizer(LUCENE_VERSION, new StringReader(query)));
        TermAttribute termAttribute = filter.getAttribute(TermAttribute.class);
        while (filter.incrementToken()) {
            result.append(termAttribute.term()).append("* ");
        }
        return result.toString();
        */
    }

    /**
     * Returns a number of random songs.
     *
     * @param criteria Search criteria.
     * @return List of random songs.
     */
    public List<MediaFile> getRandomSongs(RandomSearchCriteria criteria) {
        throw new RuntimeException("No more implemented");
/*
        List<MediaFile> result = new ArrayList<MediaFile>();

        IndexReader reader = null;
        try {
            reader = createIndexReader(SONG);
            Searcher searcher = new IndexSearcher(reader);

            BooleanQuery query = new BooleanQuery();
            query.add(new TermQuery(new Term(FIELD_MEDIA_TYPE, MediaFile.MediaType.MUSIC.name().toLowerCase())), BooleanClause.Occur.MUST);
            if (criteria.getGenre() != null) {
                String genre = normalizeGenre(criteria.getGenre());
                query.add(new TermQuery(new Term(FIELD_GENRE, genre)), BooleanClause.Occur.MUST);
            }
            if (criteria.getFromYear() != null || criteria.getToYear() != null) {
                NumericRangeQuery<Integer> rangeQuery = NumericRangeQuery.newIntRange(FIELD_YEAR, criteria.getFromYear(), criteria.getToYear(), true, true);
                query.add(rangeQuery, BooleanClause.Occur.MUST);
            }

            List<SpanTermQuery> musicFolderQueries = new ArrayList<SpanTermQuery>();
            for (MusicFolder musicFolder : criteria.getMusicFolders()) {
                musicFolderQueries.add(new SpanTermQuery(new Term(FIELD_FOLDER, musicFolder.getPath().getPath())));
            }
            query.add(new SpanOrQuery(musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()])), BooleanClause.Occur.MUST);

            TopDocs topDocs = searcher.search(query, null, Integer.MAX_VALUE);
            List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
            Random random = new Random(System.currentTimeMillis());

            while (!scoreDocs.isEmpty() && result.size() < criteria.getCount()) {
                int index = random.nextInt(scoreDocs.size());
                Document doc = searcher.doc(scoreDocs.remove(index).doc);
                int id = Integer.valueOf(doc.get(FIELD_ID));
                try {
                    addIfNotNull(mediaFileService.getMediaFile(id), result);
                } catch (Exception x) {
                    LOG.warn("Failed to get media file " + id);
                }
            }

        } catch (Throwable x) {
            LOG.error("Failed to search or random songs.", x);
        } finally {
            FileUtil.closeQuietly(reader);
        }
        return result;
        */
    }

    private static String normalizeGenre(String genre) {
        return genre.toLowerCase().replace(" ", "").replace("-", "");
    }

    private MediaFile convertFromHitAndCollect(SearchHit hit,List<MediaFile> list) throws IOException {
        String hitSource = hit.getSourceAsString();
        MediaFile mediaFile = getElasticSearchClient().getMapper().readValue(hitSource,MediaFile.class);
        list.add(mediaFile);
        return mediaFile;
    }

    /**
     * Returns a number of random albums.
     *
     * @param count        Number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return List of random albums.
     */
    public List<MediaFile> getRandomAlbums(int count, List<MusicFolder> musicFolders) {


        SearchResponse allHits = getElasticSearchClient().getClient().prepareSearch(ElasticSearchClient.SUBSONIC_MEDIA_INDEX_NAME)
                .setQuery(QueryBuilders.typeQuery(ElasticSearchClient.ALBUM_TYPE))
                .setSize(10)
                .execute().actionGet();

        List<MediaFile> list = new ArrayList<>();
        allHits.getHits().forEach(hit -> {
            try {
                convertFromHitAndCollect(hit, list);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return list;

        /*
        List<MediaFile> result = new ArrayList<MediaFile>();

        IndexReader reader = null;
        try {
            reader = createIndexReader(ALBUM);
            Searcher searcher = new IndexSearcher(reader);

            List<SpanTermQuery> musicFolderQueries = new ArrayList<SpanTermQuery>();
            for (MusicFolder musicFolder : musicFolders) {
                musicFolderQueries.add(new SpanTermQuery(new Term(FIELD_FOLDER, musicFolder.getPath().getPath())));
            }
            Query query = new SpanOrQuery(musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()]));

            TopDocs topDocs = searcher.search(query, null, Integer.MAX_VALUE);
            List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
            Random random = new Random(System.currentTimeMillis());

            while (!scoreDocs.isEmpty() && result.size() < count) {
                int index = random.nextInt(scoreDocs.size());
                Document doc = searcher.doc(scoreDocs.remove(index).doc);
                int id = Integer.valueOf(doc.get(FIELD_ID));
                try {
                    addIfNotNull(mediaFileService.getMediaFile(id), result);
                } catch (Exception x) {
                    LOG.warn("Failed to get media file " + id, x);
                }
            }

        } catch (Throwable x) {
            LOG.error("Failed to search for random albums.", x);
        } finally {
            FileUtil.closeQuietly(reader);
        }
        return result;
        */
    }

    /**
     * Returns a number of random albums, using ID3 tag.
     *
     * @param count        Number of albums to return.
     * @param musicFolders Only return albums from these folders.
     * @return List of random albums.
     */
    public List<Album> getRandomAlbumsId3(int count, List<MusicFolder> musicFolders) {
        throw new RuntimeException("No more implemented");
        /*
        List<Album> result = new ArrayList<Album>();

        IndexReader reader = null;
        try {
            reader = createIndexReader(ALBUM_ID3);
            Searcher searcher = new IndexSearcher(reader);

            List<SpanTermQuery> musicFolderQueries = new ArrayList<SpanTermQuery>();
            for (MusicFolder musicFolder : musicFolders) {
                musicFolderQueries.add(new SpanTermQuery(new Term(FIELD_FOLDER_ID, NumericUtils.intToPrefixCoded(musicFolder.getId()))));
            }
            Query query = new SpanOrQuery(musicFolderQueries.toArray(new SpanQuery[musicFolderQueries.size()]));
            TopDocs topDocs = searcher.search(query, null, Integer.MAX_VALUE);
            List<ScoreDoc> scoreDocs = Lists.newArrayList(topDocs.scoreDocs);
            Random random = new Random(System.currentTimeMillis());

            while (!scoreDocs.isEmpty() && result.size() < count) {
                int index = random.nextInt(scoreDocs.size());
                Document doc = searcher.doc(scoreDocs.remove(index).doc);
                int id = Integer.valueOf(doc.get(FIELD_ID));
                try {
                    addIfNotNull(albumDao.getAlbum(id), result);
                } catch (Exception x) {
                    LOG.warn("Failed to get album file " + id, x);
                }
            }

        } catch (Throwable x) {
            LOG.error("Failed to search for random albums.", x);
        } finally {
            FileUtil.closeQuietly(reader);
        }
        return result;
        */
    }

    private <T> void addIfNotNull(T value, List<T> list) {
        if (value != null) {
            list.add(value);
        }
    }

    private IndexWriter createIndexWriter(IndexType indexType) throws IOException {
        throw new RuntimeException("No more implemented");
        /*
        File dir = getIndexDirectory(indexType);
        return new IndexWriter(FSDirectory.open(dir), new SubsonicAnalyzer(), true, new IndexWriter.MaxFieldLength(10));
        */
    }

    private IndexReader createIndexReader(IndexType indexType) throws IOException {
        throw new RuntimeException("No more implemented");
        /*
        File dir = getIndexDirectory(indexType);
        return IndexReader.open(FSDirectory.open(dir), true);
        */
    }

    private File getIndexRootDirectory() {
        return new File(SettingsService.getSubsonicHome(), LUCENE_DIR);
    }

    private File getIndexDirectory(IndexType indexType) {
        return new File(getIndexRootDirectory(), indexType.toString().toLowerCase());
    }

    private void removeLocks() {
        throw new RuntimeException("No more implemented");
        /*
        for (IndexType indexType : IndexType.values()) {
            Directory dir = null;
            try {
                dir = FSDirectory.open(getIndexDirectory(indexType));
                if (IndexWriter.isLocked(dir)) {
                    IndexWriter.unlock(dir);
                    LOG.info("Removed Lucene lock file in " + dir);
                }
            } catch (Exception x) {
                LOG.warn("Failed to remove Lucene lock file in " + dir, x);
            } finally {
                FileUtil.closeQuietly(dir);
            }
        }
        */
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setArtistDao(ArtistDao artistDao) {
        this.artistDao = artistDao;
    }

    public void setAlbumDao(AlbumDao albumDao) {
        this.albumDao = albumDao;
    }

    public static enum IndexType {

        SONG(new String[]{FIELD_TITLE, FIELD_ARTIST}, FIELD_TITLE) {
            @Override
            public Document createDocument(MediaFile mediaFile) {
                throw new RuntimeException("No more implemented");
                /*
                Document doc = new Document();
                doc.add(new NumericField(FIELD_ID, Field.Store.YES, false).setIntValue(mediaFile.getId()));
                doc.add(new Field(FIELD_MEDIA_TYPE, mediaFile.getMediaType().name(), Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));

                if (mediaFile.getTitle() != null) {
                    doc.add(new Field(FIELD_TITLE, mediaFile.getTitle(), Field.Store.YES, Field.Index.ANALYZED));
                }
                if (mediaFile.getArtist() != null) {
                    doc.add(new Field(FIELD_ARTIST, mediaFile.getArtist(), Field.Store.YES, Field.Index.ANALYZED));
                }
                if (mediaFile.getGenre() != null) {
                    doc.add(new Field(FIELD_GENRE, normalizeGenre(mediaFile.getGenre()), Field.Store.NO, Field.Index.ANALYZED));
                }
                if (mediaFile.getYear() != null) {
                    doc.add(new NumericField(FIELD_YEAR, Field.Store.NO, true).setIntValue(mediaFile.getYear()));
                }
                if (mediaFile.getFolder() != null) {
                    doc.add(new Field(FIELD_FOLDER, mediaFile.getFolder(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                }

                return doc;
                */
            }
        },

        ALBUM(new String[]{FIELD_ALBUM, FIELD_ARTIST, FIELD_FOLDER}, FIELD_ALBUM) {
            @Override
            public Document createDocument(MediaFile mediaFile) {
                throw new RuntimeException("No more implemented");
                /*
                Document doc = new Document();
                doc.add(new NumericField(FIELD_ID, Field.Store.YES, false).setIntValue(mediaFile.getId()));

                if (mediaFile.getArtist() != null) {
                    doc.add(new Field(FIELD_ARTIST, mediaFile.getArtist(), Field.Store.YES, Field.Index.ANALYZED));
                }
                if (mediaFile.getAlbumName() != null) {
                    doc.add(new Field(FIELD_ALBUM, mediaFile.getAlbumName(), Field.Store.YES, Field.Index.ANALYZED));
                }
                if (mediaFile.getFolder() != null) {
                    doc.add(new Field(FIELD_FOLDER, mediaFile.getFolder(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                }

                return doc;
                */
            }
        },

        ALBUM_ID3(new String[]{FIELD_ALBUM, FIELD_ARTIST, FIELD_FOLDER_ID}, FIELD_ALBUM) {
            @Override
            public Document createDocument(Album album) {
                throw new RuntimeException("No more implemented");
                /*
                Document doc = new Document();
                doc.add(new NumericField(FIELD_ID, Field.Store.YES, false).setIntValue(album.getId()));

                if (album.getArtist() != null) {
                    doc.add(new Field(FIELD_ARTIST, album.getArtist(), Field.Store.YES, Field.Index.ANALYZED));
                }
                if (album.getName() != null) {
                    doc.add(new Field(FIELD_ALBUM, album.getName(), Field.Store.YES, Field.Index.ANALYZED));
                }
                if (album.getFolderId() != null) {
                    doc.add(new NumericField(FIELD_FOLDER_ID, Field.Store.NO, true).setIntValue(album.getFolderId()));
                }

                return doc;
                */
            }
        },

        ARTIST(new String[]{FIELD_ARTIST, FIELD_FOLDER}, null) {
            @Override
            public Document createDocument(MediaFile mediaFile) {
                throw new RuntimeException("No more implemented");
                /*
                Document doc = new Document();
                doc.add(new NumericField(FIELD_ID, Field.Store.YES, false).setIntValue(mediaFile.getId()));

                if (mediaFile.getArtist() != null) {
                    doc.add(new Field(FIELD_ARTIST, mediaFile.getArtist(), Field.Store.YES, Field.Index.ANALYZED));
                }
                if (mediaFile.getFolder() != null) {
                    doc.add(new Field(FIELD_FOLDER, mediaFile.getFolder(), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS));
                }

                return doc;
                */
            }
        },

        ARTIST_ID3(new String[]{FIELD_ARTIST}, null) {
            @Override
            public Document createDocument(Artist artist, MusicFolder musicFolder) {
                throw new RuntimeException("No more implemented");
                /*
                Document doc = new Document();
                doc.add(new NumericField(FIELD_ID, Field.Store.YES, false).setIntValue(artist.getId()));
                doc.add(new Field(FIELD_ARTIST, artist.getName(), Field.Store.YES, Field.Index.ANALYZED));
                doc.add(new NumericField(FIELD_FOLDER_ID, Field.Store.NO, true).setIntValue(musicFolder.getId()));

                return doc;
                */
            }
        };

        private final String[] fields;
        private final Map<String, Float> boosts;

        private IndexType(String[] fields, String boostedField) {
            this.fields = fields;
            boosts = new HashMap<String, Float>();
            if (boostedField != null) {
                boosts.put(boostedField, 2.0F);
            }
        }

        public String[] getFields() {
            return fields;
        }

        protected Document createDocument(MediaFile mediaFile) {
            throw new UnsupportedOperationException();
        }

        protected Document createDocument(Artist artist, MusicFolder musicFolder) {
            throw new UnsupportedOperationException();
        }

        protected Document createDocument(Album album) {
            throw new UnsupportedOperationException();
        }

        public Map<String, Float> getBoosts() {
            return boosts;
        }
    }

   /* private class SubsonicAnalyzer extends StandardAnalyzer {
        private SubsonicAnalyzer() {


            super(LUCENE_VERSION);

        }

       @Override
        public TokenStream tokenStream(String fieldName, Reader reader) {
            throw new RuntimeException("No more implemented");

            TokenStream result = super.tokenStream(fieldName, reader);
            return new ASCIIFoldingFilter(result);

        }


        public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
            throw new RuntimeException("No more implemented");

            class SavedStreams {
                StandardTokenizer tokenStream;
                TokenStream filteredTokenStream;
            }

            SavedStreams streams = (SavedStreams) getPreviousTokenStream();
            if (streams == null) {
                streams = new SavedStreams();
                setPreviousTokenStream(streams);
                streams.tokenStream = new StandardTokenizer(LUCENE_VERSION, reader);
                streams.filteredTokenStream = new StandardFilter(streams.tokenStream);
                streams.filteredTokenStream = new LowerCaseFilter(streams.filteredTokenStream);
                streams.filteredTokenStream = new StopFilter(true, streams.filteredTokenStream, STOP_WORDS_SET);
                streams.filteredTokenStream = new ASCIIFoldingFilter(streams.filteredTokenStream);
            } else {
                streams.tokenStream.reset(reader);
            }
            streams.tokenStream.setMaxTokenLength(DEFAULT_MAX_TOKEN_LENGTH);

            return streams.filteredTokenStream;

        }
    } */

    public ElasticSearchClient getElasticSearchClient() {
        return elasticSearchClient;
    }

    public void setElasticSearchClient(ElasticSearchClient elasticSearchClient) {
        this.elasticSearchClient = elasticSearchClient;
    }
}


