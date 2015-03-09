<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>
<!DOCTYPE HTML>
<html>
<%@ include file="head.jsp" %>

<body>

<c:import url="header.jsp"/>

<section id="main" class="container">
<header>
    <h2>Change Log</h2>
</header>
<div class="row">
<div class="12u">

<section class="box">

    <a name="5.2"><h3>Subsonic 5.2 - Feb 24, 2015</h3></a>
    <ul>
        <li><strong>New:</strong> Added option to save/load current play queue, including current song and position.</li>
        <li><strong>New:</strong> Bundle Java 8 JRE with Mac installer.</li>
        <li><strong>New:</strong> Use Java 8 for Windows installer.</li>
        <li><strong>Bugfix:</strong> Fixed bug in Settings &gt; Users that occured with Java 8.</li>
        <li><strong>Bugfix:</strong> Fixed broken global shuffle play in Sonos.</li>
        <li><strong>Bugfix:</strong> Make scrobbling work for Sonos.</li>
        <li><strong>REST:</strong> Added savePlayQueue, getPlayQueue.</li>
    </ul>

    <a name="5.2.beta1"><h3>Subsonic 5.2.beta1 - Feb 14, 2015</h3></a>
    <ul>
        <li><strong>New:</strong> <a href="sonos.jsp">Sonos</a> integration. Use Subsonic as a music service in Sonos.</li>
        <li><strong>New:</strong> Media folder access management. Configure which folders each user is allowed to access.</li>
        <li><strong>New:</strong> Use 192 instead of 128 kbps as default bitrate when transcoding.</li>
        <li><strong>New:</strong> Updated year list in More &gt; Shuffle play.</li>
        <li><strong>Bugfix:</strong> Avoid duplicates in random album/song lists.</li>
        <li><strong>REST:</strong> Support for administering folder access (createUser, updateUser, getUser, getUsers).</li>
        <li><strong>REST:</strong> Added musicFolderId parameter to getAlbumList2, getSongsByGenre, getStarred, getStarred2, search2, search3.</li>
        <li><strong>Tech:</strong> Added debug logging to sql queries that takes more than two seconds.</li>
        <li><strong>Tech:</strong> Only log debug if beta.</li>
    </ul>

    <a name="5.1"><h3>Subsonic 5.1 - Jan 18, 2015</h3></a>
    <ul>
        <li><strong>Bugfix:</strong> Misc improvements to list mode.</li>
    </ul>

    <a name="5.1.beta2"><h3>Subsonic 5.1.beta2 - Jan 11, 2015</h3></a>
    <ul>
        <li><strong>New:</strong> Drag and drop to rearrange playlists and play queue.</li>
        <li><strong>New:</strong> Added buttons for toggling between thumbnail and list mode.</li>
        <li><strong>New:</strong> Don't remove songs from playlists if they become unavailable.</li>
        <li><strong>New:</strong> Show toast when adding songs to queue.</li>
        <li><strong>New:</strong> Nicer display of "No media folders found", "New version available" etc</li>
        <li><strong>New:</strong> Make title clickable in video thumb</li>
        <li><strong>New:</strong> Show song count and duration in play queue.</li>
        <li><strong>New:</strong> New favicon</li>
        <li><strong>New:</strong> Support starring/unstarring videos.</li>
        <li><strong>New:</strong> Added video category in "Starred".</li>
        <li><strong>Bugfix:</strong> Show correct "siebling" albums.</li>
        <li><strong>REST:</strong> scrobble method now also updates play statistic and the list of "now playing" songs.</li>
    </ul>

    <a name="5.1.beta1"><h3>Subsonic 5.1.beta1 - Dec 29, 2014</h3></a>
    <ul>
        <li><strong>New:</strong> New video page, with preview thumbs.</li>
        <li><strong>New:</strong> New playlists page, with cover art.</li>
        <li><strong>New:</strong> Automatically show/hide play queue.</li>
        <li><strong>New:</strong> Show artist image and bio.</li>
        <li><strong>New:</strong> Show similar artists, with radio feature.</li>
        <li><strong>New:</strong> Instant search (show search results while typing)</li>
        <li><strong>New:</strong> Refreshed design.</li>
        <li><strong>New:</strong> Added "shuffle play" to album lists in home page.</li>
        <li><strong>New:</strong> Show song title in browser window title.</li>
        <li><strong>New:</strong> Filter home screen by selected music folder</li>
        <li><strong>New:</strong> Auto-start videos.</li>
        <li><strong>New:</strong> Sort artists using locale-specific collation.</li>
        <li><strong>New:</strong> Updated Estonian translation, courtesy of Olav M&auml;gi.</li>
        <li><strong>New:</strong> Show avatar in upper-right corner.</li>
        <li><strong>New:</strong> Removed external ads.</li>
        <li><strong>Bugfix:</strong> Video playback broken behind proxies.</li>
        <li><strong>Bugfix:</strong> Wrong aspect rate for shared videos.</li>
        <li><strong>Bugfix:</strong> Configure Windows firewall for all network profiles.</li>
        <li><strong>REST:</strong> Added getSimilarSongs and getArtistInfo.</li>
        <li><strong>REST:</strong> Support playlist cover art</li>
        <li><strong>REST:</strong> Added musicFolderId parameter to getAlbumList</li>
    </ul>

    <a name="5.0"><h3>Subsonic 5.0 - Sep 21, 2014</h3></a>
    <ul>
        <li><strong>Bugfix:</strong> Use UTF-8 encoding for filenames in ZIP archives. (Requires Java 7+)</li>
        <li><strong>Bugfix:</strong> Fixed problem with unresponsive dialogs in web interface.</li>
        <li><strong>Bugfix:</strong> Use 2000 kbps as default video bitrate.</li>
        <li><strong>Bugfix:</strong> Sort playlists alphabetically.</li>
        <li><strong>Bugfix:</strong> Fixed some sorting issues (e.g., for same artist found in multiple media folders).</li>
        <li><strong>Bugfix:</strong> Make password recovery work with https.</li>
    </ul>

    <a name="5.0.beta2"><h3>Subsonic 5.0.beta2 - Aug 26, 2014</h3></a>
    <ul>
        <li><strong>New:</strong> Support casting to remote Chromecasts (requires *.subsonic.org address)</li>
        <li><strong>New:</strong> Added video bitrate selector.</li>
        <li><strong>New:</strong> Make DLNA media server name configurable.</li>
        <li><strong>New:</strong> Updated Czech translation, courtesy of Trottel.</li>
        <li><strong>New:</strong> Enable compression for XML, JSON and JSONP in the REST API</li>
        <li><strong>New:</strong> Set X-Content-Duration to support opus encoding.</li>
        <li><strong>Bugfix:</strong> Fixed problem with whole tab being sent to Chromecast rather than just the video.</li>
        <li><strong>Bugfix:</strong> Access-Control-Allow-Origin header sometimes missing in REST responses.</li>
        <li><strong>Bugfix:</strong> Fixed DLNA recursion bug.</li>
        <li><strong>Bugfix:</strong> Fixed mixed content errors when changing cover art, and using https (courtesy of daneren2005)</li>
        <li><strong>Bugfix:</strong> Fixed errors on certain podcast feeds where they were being incorrectly labeled as video files (courtesy of daneren2005)</li>
        <li><strong>Bugfix:</strong> Updated ffmpeg commands to fix some transcoding issues.</li>
        <li><strong>Bugfix:</strong> Fixed bug in REST method changePassword</li>
        <li><strong>Bugfix:</strong> Avoid creating duplicate players when switching from Chromecast to local.</li>
    </ul>

    <%@ include file="changelog-older.jsp" %>


</section>
</div>
</div>
</section>

<%@ include file="footer.jsp" %>

</body>
</html>