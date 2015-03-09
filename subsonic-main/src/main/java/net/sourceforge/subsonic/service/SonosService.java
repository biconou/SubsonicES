/*
 * This file is part of Subsonic.
 *
 *  Subsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Subsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */

package net.sourceforge.subsonic.service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.context.WrappedMessageContext;
import org.apache.cxf.message.Message;
import org.w3c.dom.Node;

import com.sonos.services._1.AbstractMedia;
import com.sonos.services._1.AddToContainerResult;
import com.sonos.services._1.ContentKey;
import com.sonos.services._1.CreateContainerResult;
import com.sonos.services._1.Credentials;
import com.sonos.services._1.DeleteContainerResult;
import com.sonos.services._1.DeviceAuthTokenResult;
import com.sonos.services._1.DeviceLinkCodeResult;
import com.sonos.services._1.ExtendedMetadata;
import com.sonos.services._1.GetExtendedMetadata;
import com.sonos.services._1.GetExtendedMetadataResponse;
import com.sonos.services._1.GetExtendedMetadataText;
import com.sonos.services._1.GetExtendedMetadataTextResponse;
import com.sonos.services._1.GetMediaMetadata;
import com.sonos.services._1.GetMediaMetadataResponse;
import com.sonos.services._1.GetMetadata;
import com.sonos.services._1.GetMetadataResponse;
import com.sonos.services._1.GetSessionId;
import com.sonos.services._1.GetSessionIdResponse;
import com.sonos.services._1.HttpHeaders;
import com.sonos.services._1.LastUpdate;
import com.sonos.services._1.MediaCollection;
import com.sonos.services._1.MediaList;
import com.sonos.services._1.MediaMetadata;
import com.sonos.services._1.RateItem;
import com.sonos.services._1.RateItemResponse;
import com.sonos.services._1.RelatedBrowse;
import com.sonos.services._1.RemoveFromContainerResult;
import com.sonos.services._1.RenameContainerResult;
import com.sonos.services._1.ReorderContainerResult;
import com.sonos.services._1.ReportPlaySecondsResult;
import com.sonos.services._1.Search;
import com.sonos.services._1.SearchResponse;
import com.sonos.services._1.SegmentMetadataList;
import com.sonos.services._1_1.SonosSoap;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.service.sonos.AlbumListType;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.service.sonos.SonosHelper;
import net.sourceforge.subsonic.service.sonos.SonosServiceRegistration;
import net.sourceforge.subsonic.service.sonos.SonosSoapFault;

/**
 * For manual testing of this service:
 * curl -s -X POST -H "Content-Type: text/xml;charset=UTF-8" -H 'SOAPACTION: "http://www.sonos.com/Services/1.1#getSessionId"' -d @getSessionId.xml http://localhost:4040/ws/Sonos | xmllint --format -
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class SonosService implements SonosSoap {

    private static final Logger LOG = Logger.getLogger(SonosService.class);

    public static final String ID_ROOT = "root";
    public static final String ID_SHUFFLE = "shuffle";
    public static final String ID_ALBUMLISTS = "albumlists";
    public static final String ID_PLAYLISTS = "playlists";
    public static final String ID_PODCASTS = "podcasts";
    public static final String ID_LIBRARY = "library";
    public static final String ID_STARRED = "starred";
    public static final String ID_STARRED_ARTISTS = "starred-artists";
    public static final String ID_STARRED_ALBUMS = "starred-albums";
    public static final String ID_STARRED_SONGS = "starred-songs";
    public static final String ID_SEARCH = "search";
    public static final String ID_SHUFFLE_MUSICFOLDER_PREFIX = "shuffle-musicfolder:";
    public static final String ID_SHUFFLE_ARTIST_PREFIX = "shuffle-artist:";
    public static final String ID_SHUFFLE_ALBUMLIST_PREFIX = "shuffle-albumlist:";
    public static final String ID_RADIO_ARTIST_PREFIX = "radio-artist:";
    public static final String ID_MUSICFOLDER_PREFIX = "musicfolder:";
    public static final String ID_PLAYLIST_PREFIX = "playlist:";
    public static final String ID_ALBUMLIST_PREFIX = "albumlist:";
    public static final String ID_PODCAST_CHANNEL_PREFIX = "podcast-channel:";
    public static final String ID_DECADE_PREFIX = "decade:";
    public static final String ID_GENRE_PREFIX = "genre:";
    public static final String ID_SIMILAR_ARTISTS_PREFIX = "similarartists:";

    // Note: These must match the values in presentationMap.xml
    public static final String ID_SEARCH_ARTISTS = "search-artists";
    public static final String ID_SEARCH_ALBUMS = "search-albums";
    public static final String ID_SEARCH_SONGS = "search-songs";

    private SonosHelper sonosHelper;
    private MediaFileService mediaFileService;
    private SecurityService securityService;
    private SettingsService settingsService;
    private UPnPService upnpService;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    /**
     * The context for the request. This is used to get the Auth information
     * form the headers as well as using the request url to build the correct
     * media resource url.
     */
    @Resource
    private WebServiceContext context;

    private String localIp;

    public void init() {
        executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                registerIfLocalIpChanged();
            }
        }, 8, 60, TimeUnit.SECONDS);
    }

    private void registerIfLocalIpChanged() {
        if (settingsService.isSonosEnabled()) {
            if (localIp == null || !localIp.equals(settingsService.getLocalIpAddress())) {
                localIp = settingsService.getLocalIpAddress();
                setMusicServiceEnabled(true);
            }
        }
    }

    public void setMusicServiceEnabled(boolean enabled) {
        List<String> sonosControllers = upnpService.getSonosControllerHosts();
        if (sonosControllers.isEmpty()) {
            LOG.info("No Sonos controller found");
            return;
        }
        LOG.info("Found Sonos controllers: " + sonosControllers);

        String sonosServiceName = settingsService.getSonosServiceName();
        int sonosServiceId = settingsService.getSonosServiceId();
        String subsonicBaseUrl = sonosHelper.getBaseUrl();

        for (String sonosController : sonosControllers) {
            try {
                new SonosServiceRegistration().setEnabled(subsonicBaseUrl, sonosController, enabled,
                                                          sonosServiceName, sonosServiceId);
                break;
            } catch (IOException x) {
                LOG.warn(String.format("Failed to enable/disable music service in Sonos controller %s: %s", sonosController, x));
            }
        }
    }


    @Override
    public LastUpdate getLastUpdate() {
        LastUpdate result = new LastUpdate();
        // Effectively disabling caching
        result.setCatalog(RandomStringUtils.randomAlphanumeric(8));
        result.setFavorites(RandomStringUtils.randomAlphanumeric(8));
        return result;
    }

    @Override
    public GetMetadataResponse getMetadata(GetMetadata parameters) {
        String id = parameters.getId();
        int index = parameters.getIndex();
        int count = parameters.getCount();
        String username = getUsername();

        LOG.debug(String.format("getMetadata: id=%s index=%s count=%s recursive=%s", id, index, count, parameters.isRecursive()));

        List<? extends AbstractMedia> media = null;
        MediaList mediaList = null;

        if (ID_ROOT.equals(id)) {
            media = sonosHelper.forRoot();
        } else {
            if (ID_SHUFFLE.equals(id)) {
                media = sonosHelper.forShuffle(count, username);
            } else if (ID_LIBRARY.equals(id)) {
                media = sonosHelper.forLibrary(username);
            } else if (ID_PLAYLISTS.equals(id)) {
                media = sonosHelper.forPlaylists(username);
            } else if (ID_ALBUMLISTS.equals(id)) {
                media = sonosHelper.forAlbumLists();
            } else if (ID_PODCASTS.equals(id)) {
                media = sonosHelper.forPodcastChannels();
            } else if (ID_STARRED.equals(id)) {
                media = sonosHelper.forStarred();
            } else if (ID_STARRED_ARTISTS.equals(id)) {
                media = sonosHelper.forStarredArtists(username);
            } else if (ID_STARRED_ALBUMS.equals(id)) {
                media = sonosHelper.forStarredAlbums(username);
            } else if (ID_STARRED_SONGS.equals(id)) {
                media = sonosHelper.forStarredSongs(username);
            } else if (ID_SEARCH.equals(id)) {
                media = sonosHelper.forSearchCategories();
            } else if (id.startsWith(ID_PLAYLIST_PREFIX)) {
                int playlistId = Integer.parseInt(id.replace(ID_PLAYLIST_PREFIX, ""));
                media = sonosHelper.forPlaylist(playlistId, username);
            } else if (id.startsWith(ID_DECADE_PREFIX)) {
                int decade = Integer.parseInt(id.replace(ID_DECADE_PREFIX, ""));
                media = sonosHelper.forDecade(decade, username);
            } else if (id.startsWith(ID_GENRE_PREFIX)) {
                int genre = Integer.parseInt(id.replace(ID_GENRE_PREFIX, ""));
                media = sonosHelper.forGenre(genre, username);
            } else if (id.startsWith(ID_ALBUMLIST_PREFIX)) {
                AlbumListType albumListType = AlbumListType.fromId(id.replace(ID_ALBUMLIST_PREFIX, ""));
                mediaList = sonosHelper.forAlbumList(albumListType, index, count, username);
            } else if (id.startsWith(ID_PODCAST_CHANNEL_PREFIX)) {
                int channelId = Integer.parseInt(id.replace(ID_PODCAST_CHANNEL_PREFIX, ""));
                media = sonosHelper.forPodcastChannel(channelId, username);
            } else if (id.startsWith(ID_MUSICFOLDER_PREFIX)) {
                int musicFolderId = Integer.parseInt(id.replace(ID_MUSICFOLDER_PREFIX, ""));
                media = sonosHelper.forMusicFolder(musicFolderId, username);
            } else if (id.startsWith(ID_SHUFFLE_MUSICFOLDER_PREFIX)) {
                int musicFolderId = Integer.parseInt(id.replace(ID_SHUFFLE_MUSICFOLDER_PREFIX, ""));
                media = sonosHelper.forShuffleMusicFolder(musicFolderId, count, username);
            } else if (id.startsWith(ID_SHUFFLE_ARTIST_PREFIX)) {
                int mediaFileId = Integer.parseInt(id.replace(ID_SHUFFLE_ARTIST_PREFIX, ""));
                media = sonosHelper.forShuffleArtist(mediaFileId, count, username);
            } else if (id.startsWith(ID_SHUFFLE_ALBUMLIST_PREFIX)) {
                AlbumListType albumListType = AlbumListType.fromId(id.replace(ID_SHUFFLE_ALBUMLIST_PREFIX, ""));
                media = sonosHelper.forShuffleAlbumList(albumListType, count, username);
            } else if (id.startsWith(ID_RADIO_ARTIST_PREFIX)) {
                int mediaFileId = Integer.parseInt(id.replace(ID_RADIO_ARTIST_PREFIX, ""));
                media = sonosHelper.forRadioArtist(mediaFileId, count, username);
            } else if (id.startsWith(ID_SIMILAR_ARTISTS_PREFIX)) {
                int mediaFileId = Integer.parseInt(id.replace(ID_SIMILAR_ARTISTS_PREFIX, ""));
                media = sonosHelper.forSimilarArtists(mediaFileId, username);
            } else {
                media = sonosHelper.forDirectoryContent(Integer.parseInt(id), username);
            }
        }

        if (mediaList == null) {
            mediaList = SonosHelper.createSubList(index, count, media);
        }

        LOG.debug(String.format("getMetadata result: id=%s index=%s count=%s total=%s",
                                id, mediaList.getIndex(), mediaList.getCount(), mediaList.getTotal()));

        GetMetadataResponse response = new GetMetadataResponse();
        response.setGetMetadataResult(mediaList);
        return response;
    }

    @Override
    public GetExtendedMetadataResponse getExtendedMetadata(GetExtendedMetadata parameters) {
        LOG.debug("getExtendedMetadata: " + parameters.getId());

        int id = Integer.parseInt(parameters.getId());
        MediaFile mediaFile = mediaFileService.getMediaFile(id);
        AbstractMedia abstractMedia = sonosHelper.forMediaFile(mediaFile, getUsername());

        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        if (abstractMedia instanceof MediaCollection) {
            extendedMetadata.setMediaCollection((MediaCollection) abstractMedia);
        } else {
            extendedMetadata.setMediaMetadata((MediaMetadata) abstractMedia);
        }

        RelatedBrowse relatedBrowse = new RelatedBrowse();
        relatedBrowse.setType("RELATED_ARTISTS");
        relatedBrowse.setId(ID_SIMILAR_ARTISTS_PREFIX + id);
        extendedMetadata.getRelatedBrowse().add(relatedBrowse);

        GetExtendedMetadataResponse response = new GetExtendedMetadataResponse();
        response.setGetExtendedMetadataResult(extendedMetadata);
        return response;
    }


    @Override
    public SearchResponse search(Search parameters) {
        String id = parameters.getId();

        SearchService.IndexType indexType;
        if (ID_SEARCH_ARTISTS.equals(id)) {
            indexType = SearchService.IndexType.ARTIST;
        } else if (ID_SEARCH_ALBUMS.equals(id)) {
            indexType = SearchService.IndexType.ALBUM;
        } else if (ID_SEARCH_SONGS.equals(id)) {
            indexType = SearchService.IndexType.SONG;
        } else {
            throw new IllegalArgumentException("Invalid search category: " + id);
        }

        MediaList mediaList = sonosHelper.forSearch(parameters.getTerm(), parameters.getIndex(),
                                                    parameters.getCount(), getUsername(), indexType);
        SearchResponse response = new SearchResponse();
        response.setSearchResult(mediaList);
        return response;
    }

    @Override
    public GetSessionIdResponse getSessionId(GetSessionId parameters) {
        LOG.debug("getSessionId: " + parameters.getUsername());
        User user = securityService.getUserByName(parameters.getUsername());
        if (user == null || !StringUtils.equals(user.getPassword(), parameters.getPassword())) {
            throw new SonosSoapFault.LoginInvalid();
        }

        if (!settingsService.getLicenseInfo().isLicenseOrTrialValid()) {
            throw new SonosSoapFault.LoginUnauthorized();
        }

        // Use username as session ID for easy access to it later.
        GetSessionIdResponse result = new GetSessionIdResponse();
        result.setGetSessionIdResult(user.getUsername());
        return result;
    }

    @Override
    public GetMediaMetadataResponse getMediaMetadata(GetMediaMetadata parameters) {
        LOG.debug("getMediaMetadata: " + parameters.getId());

        int id = Integer.parseInt(parameters.getId());
        MediaFile song = mediaFileService.getMediaFile(id);

        GetMediaMetadataResponse response = new GetMediaMetadataResponse();
        GetMediaMetadataResponse.GetMediaMetadataResult result = new GetMediaMetadataResponse.GetMediaMetadataResult();
        result.setMediaMetadata(sonosHelper.forSong(song, getUsername()));
        response.setGetMediaMetadataResult(result);

        return response;
    }

    @Override
    public void getMediaURI(String id, Holder<String> result, Holder<HttpHeaders> httpHeaders, Holder<Integer> uriTimeout) {
        result.value = sonosHelper.getMediaURI(Integer.parseInt(id), getUsername());
        LOG.debug("getMediaURI: " + id + " -> " + result.value);
    }

    @Override
    public String createItem(String favorite) {
        int id = Integer.parseInt(favorite);
        sonosHelper.star(id, getUsername());
        return favorite;
    }

    @Override
    public void deleteItem(String favorite) {
        int id = Integer.parseInt(favorite);
        sonosHelper.unstar(id, getUsername());
    }

    private String getUsername() {
        MessageContext messageContext = context.getMessageContext();
        if (messageContext == null || !(messageContext instanceof WrappedMessageContext)) {
            LOG.error("Message context is null or not an instance of WrappedMessageContext.");
            return null;
        }

        Message message = ((WrappedMessageContext) messageContext).getWrappedMessage();
        List<Header> headers = CastUtils.cast((List<?>) message.get(Header.HEADER_LIST));
        if (headers != null) {
            for (Header h : headers) {
                Object o = h.getObject();
                // Unwrap the node using JAXB
                if (o instanceof Node) {
                    JAXBContext jaxbContext;
                    try {
                        // TODO: Check performance
                        jaxbContext = new JAXBDataBinding(Credentials.class).getContext();
                        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
                        o = unmarshaller.unmarshal((Node) o);
                    } catch (JAXBException e) {
                        // failed to get the credentials object from the headers
                        LOG.error("JAXB error trying to unwrap credentials", e);
                    }
                }
                if (o instanceof Credentials) {
                    Credentials c = (Credentials) o;

                    // Note: We're using the username as session ID.
                    String username = c.getSessionId();
                    if (username == null) {
                        LOG.debug("No session id in credentials object, get from login");
                        username = c.getLogin().getUsername();
                    }
                    return username;
                } else {
                    LOG.error("No credentials object");
                }
            }
        } else {
            LOG.error("No headers found");
        }
        return null;
    }

    public void setSonosHelper(SonosHelper sonosHelper) {
        this.sonosHelper = sonosHelper;
    }

    @Override
    public RateItemResponse rateItem(RateItem parameters) {
        return null;
    }

    @Override
    public CreateContainerResult createContainer(String containerType, String title, String parentId, String seedId) {
        return null;
    }

    @Override
    public AddToContainerResult addToContainer(String id, String parentId, int index, String updateId) {
        return null;
    }

    @Override
    public RenameContainerResult renameContainer(String id, String title) {
        return null;
    }

    @Override
    public SegmentMetadataList getStreamingMetadata(String id, XMLGregorianCalendar startTime, int duration) {
        return null;
    }

    @Override
    public ReorderContainerResult reorderContainer(String id, String from, int to, String updateId) {
        return null;
    }

    @Override
    public GetExtendedMetadataTextResponse getExtendedMetadataText(GetExtendedMetadataText parameters) {
        return null;
    }

    @Override
    public DeviceLinkCodeResult getDeviceLinkCode(String householdId) {
        return null;
    }

    @Override
    public void reportAccountAction(String type) {

    }

    @Override
    public void setPlayedSeconds(String id, int seconds) {

    }

    @Override
    public ReportPlaySecondsResult reportPlaySeconds(String id, int seconds) {
        return null;
    }

    @Override
    public DeviceAuthTokenResult getDeviceAuthToken(String householdId, String linkCode, String linkDeviceId) {
        return null;
    }

    @Override
    public void reportStatus(String id, int errorCode, String message) {
    }

    @Override
    public String getScrollIndices(String id) {
        return null;
    }

    @Override
    public DeleteContainerResult deleteContainer(String id) {
        return null;
    }

    @Override
    public void reportPlayStatus(String id, String status) {

    }

    @Override
    public ContentKey getContentKey(String id, String uri) {
        return null;
    }

    @Override
    public RemoveFromContainerResult removeFromContainer(String id, String indices, String updateId) {
        return null;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setUpnpService(UPnPService upnpService) {
        this.upnpService = upnpService;
    }
}
