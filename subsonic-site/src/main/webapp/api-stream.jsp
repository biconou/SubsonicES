<a name="stream"></a>
<section class="box">
    <h3>stream</h3>

    <p>
        <code>http://your-server/rest/stream.view</code>
        Since <a href="#versions">1.0.0</a>
    </p>

    <p>
        Streams a given media file.
    </p>
    <table>
        <tr>
            <th>Parameter</th>
            <th>Required</th>
            <th>Default</th>
            <th>Comment</th>
        </tr>
        <tr>
            <td><code>id</code></td>
            <td>Yes</td>
            <td></td>
            <td>A string which uniquely identifies the file to stream. Obtained by calls to getMusicDirectory.</td>
        </tr>
        <tr>
            <td><code>maxBitRate</code></td>
            <td>No</td>
            <td></td>
            <td>(Since <a href="#versions">1.2.0</a>) If specified, the server will attempt to limit the bitrate
                to this value, in kilobits per second. If set to zero, no limit is imposed.
            </td>
        </tr>
        <tr>
            <td><code>format</code></td>
            <td>No</td>
            <td></td>
            <td>(Since <a href="#versions">1.6.0</a>) Specifies the preferred target format (e.g., "mp3" or "flv") in
                case
                there
                are multiple applicable transcodings. Starting with <a href="#versions">1.9.0</a> you can use the
                special
                value "raw" to disable transcoding.
            </td>
        </tr>
        <tr>
            <td><code>timeOffset</code></td>
            <td>No</td>
            <td></td>
            <td>Only applicable to video streaming. If specified, start streaming at the given offset (in seconds) into
                the
                video.
                Typically used to implement video skipping.
            </td>
        </tr>
        <tr>
            <td><code>size</code></td>
            <td>No</td>
            <td></td>
            <td>(Since <a href="#versions">1.6.0</a>) Only applicable to video streaming. Requested video size specified
                as
                WxH, for instance "640x480".
            </td>
        </tr>
        <tr>
            <td><code>estimateContentLength</code></td>
            <td>No</td>
            <td>false</td>
            <td>(Since <a href="#versions">1.8.0</a>). If set to "true", the <em>Content-Length</em> HTTP header will be
                set
                to an estimated value
                for transcoded or downsampled media.
            </td>
        </tr>
    </table>
    <p>
        Returns binary data on success, or an XML document on error (in which case the HTTP content type will start with
        "text/xml").
    </p>
</section>