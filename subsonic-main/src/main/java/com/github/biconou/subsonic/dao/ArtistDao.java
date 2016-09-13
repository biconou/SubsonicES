package com.github.biconou.subsonic.dao;

import net.sourceforge.subsonic.dao.MusicFolderDao;
import net.sourceforge.subsonic.domain.Artist;
import net.sourceforge.subsonic.domain.Genre;
import net.sourceforge.subsonic.domain.MusicFolder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Created by remi on 12/09/16.
 */
public class ArtistDao extends net.sourceforge.subsonic.dao.ArtistDao {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AlbumDao.class);

    private ElasticSearchDaoHelper elasticSearchDaoHelper = null;

    private MusicFolderDao musicFolderDao = null;


    public void setMusicFolderDao(MusicFolderDao musicFolderDao) {
        this.musicFolderDao = musicFolderDao;
    }

    public ElasticSearchDaoHelper getElasticSearchDaoHelper() {
        return elasticSearchDaoHelper;
    }

    public void setElasticSearchDaoHelper(ElasticSearchDaoHelper elasticSearchDaoHelper) {
        this.elasticSearchDaoHelper = elasticSearchDaoHelper;
    }




    @Override
    public List<Artist> getAlphabetialArtists(int offset, int count, List<MusicFolder> musicFolders) {

        List<Artist> artists = new ArrayList<>();

        SearchResponse genresResponse = elasticSearchDaoHelper.getClient().prepareSearch(elasticSearchDaoHelper.indexNames(musicFolders))
                .setQuery(QueryBuilders.typeQuery("MEDIA_FILE"))
                .addAggregation(AggregationBuilders.terms("artist_agg").field("artist").order(Terms.Order.aggregation("_term",true))
                        .subAggregation(AggregationBuilders.terms("mediaType_agg").field("mediaType"))).setSize(0).get();



        StringTerms artistAgg = genresResponse.getAggregations().get("artist_agg");
        for (Terms.Bucket entry : artistAgg.getBuckets()) {
            String artistName = entry.getKeyAsString();
            Artist artist = new Artist();
            artists.add(artist);
            artist.setName(artistName);
            StringTerms mediaTypeAgg = entry.getAggregations().get("mediaType_agg");
            for (Terms.Bucket mediaTypeEntry : mediaTypeAgg.getBuckets()) {
                String mediaTypeKey = mediaTypeEntry.getKeyAsString();
                if ("ALBUM".equals(mediaTypeKey)) {
                    artist.setAlbumCount((int)mediaTypeEntry.getDocCount());
                }
            }
        }

      /*  List<Genre> genres = new ArrayList<>();
        artistsMap.keySet().forEach(genreKey -> genres.add(artistsMap.get(genreKey)));
        if (sortByAlbum) {
            genres.sort((o1, o2) -> {
                if (o1.getAlbumCount() > o2.getAlbumCount()) {
                    return -1;
                }
                if (o1.getAlbumCount() < o2.getAlbumCount()) {
                    return 1;
                }
                return 0;
            });
        } else {
            genres.sort((o1, o2) -> {
                if (o1.getSongCount() > o2.getSongCount()) {
                    return -1;
                }
                if (o1.getSongCount() < o2.getSongCount()) {
                    return 1;
                }
                return 0;
            });
        } */
        return artists;
    }
}
