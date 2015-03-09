<a name="createPlaylist"></a>
<section class="box">
    <h3>createPlaylist</h3>

    <p>
        <code>http://your-server/rest/createPlaylist.view</code>
        Since <a href="#versions">1.2.0</a>
    </p>

    <p>
        Creates (or updates) a playlist.
    </p>
    <table>
        <tr>
            <th>Parameter</th>
            <th>Required</th>
            <th>Default</th>
            <th>Comment</th>
        </tr>
        <tr>
            <td><code>playlistId</code></td>
            <td>Yes (if updating)</td>
            <td></td>
            <td>The playlist ID.</td>
        </tr>
        <tr>
            <td><code>name</code></td>
            <td>Yes (if creating)</td>
            <td></td>
            <td>The human-readable name of the playlist.</td>
        </tr>
        <tr>
            <td><code>songId</code></td>
            <td>Yes</td>
            <td></td>
            <td>ID of a song in the playlist. Use one <code>songId</code> parameter for each song in the playlist.</td>
        </tr>
    </table>
    <p>
        Returns an empty <code>&lt;subsonic-response&gt;</code> element on success.
    </p>
</section>