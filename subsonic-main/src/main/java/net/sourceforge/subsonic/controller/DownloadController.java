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
package net.sourceforge.subsonic.controller;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.PlayQueue;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.Playlist;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.io.RangeOutputStream;
import net.sourceforge.subsonic.service.MediaFileService;
import net.sourceforge.subsonic.service.PlayerService;
import net.sourceforge.subsonic.service.PlaylistService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.service.StatusService;
import net.sourceforge.subsonic.util.FileUtil;
import net.sourceforge.subsonic.util.StringUtil;
import net.sourceforge.subsonic.util.Util;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.LongRange;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;
import org.springframework.web.servlet.mvc.LastModified;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

/**
 * A controller used for downloading files to a remote client. If the requested path refers to a file, the
 * given file is downloaded.  If the requested path refers to a directory, the entire directory (including
 * sub-directories) are downloaded as an uncompressed zip-file.
 *
 * @author Sindre Mehus
 */
public class DownloadController implements Controller, LastModified {

    private static final Logger LOG = Logger.getLogger(DownloadController.class);

    private PlayerService playerService;
    private StatusService statusService;
    private SecurityService securityService;
    private PlaylistService playlistService;
    private SettingsService settingsService;
    private MediaFileService mediaFileService;

    public long getLastModified(HttpServletRequest request) {
        try {
            MediaFile mediaFile = getMediaFile(request);
            if (mediaFile == null || mediaFile.isDirectory() || mediaFile.getChanged() == null) {
                return -1;
            }
            return mediaFile.getChanged().getTime();
        } catch (ServletRequestBindingException e) {
            return -1;
        }
    }

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

        TransferStatus status = null;
        try {

            status = statusService.createDownloadStatus(playerService.getPlayer(request, response, false, false));

            MediaFile mediaFile = getMediaFile(request);
            Integer playlistId = ServletRequestUtils.getIntParameter(request, "playlist");
            String playerId = request.getParameter("player");
            int[] indexes = request.getParameter("i") == null ? null : ServletRequestUtils.getIntParameters(request, "i");

            if (mediaFile != null) {
                response.setIntHeader("ETag", mediaFile.getId());
                response.setHeader("Accept-Ranges", "bytes");
            }

            LongRange range = StringUtil.parseRange(request.getHeader("Range"));
            if (range != null) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                LOG.info("Got range: " + range);
            }

            if (mediaFile != null) {
                if (mediaFile.isFile()) {
                    downloadFile(response, status, mediaFile.getFile(), range);
                } else {
                    List<MediaFile> children = mediaFileService.getChildrenOf(mediaFile, true, true, true);
                    String zipFileName = FilenameUtils.getBaseName(mediaFile.getPath()) + ".zip";
                    downloadFiles(response, status, children, indexes, range, zipFileName);
                }

            } else if (playlistId != null) {
                List<MediaFile> songs = playlistService.getFilesInPlaylist(playlistId);
                Playlist playlist = playlistService.getPlaylist(playlistId);
                downloadFiles(response, status, songs, null, range, playlist.getName() + ".zip");

            } else if (playerId != null) {
                Player player = playerService.getPlayerById(playerId);
                PlayQueue playQueue = player.getPlayQueue();
                playQueue.setName("Playlist");
                downloadFiles(response, status, playQueue.getFiles(), indexes, range, "download.zip");
            }

        } finally {
            if (status != null) {
                statusService.removeDownloadStatus(status);
                User user = securityService.getCurrentUser(request);
                securityService.updateUserByteCounts(user, 0L, status.getBytesTransfered(), 0L);
            }
        }

        return null;
    }

    private MediaFile getMediaFile(HttpServletRequest request) throws ServletRequestBindingException {
        Integer id = ServletRequestUtils.getIntParameter(request, "id");
        return id == null ? null : mediaFileService.getMediaFile(id);
    }

    /**
     * Downloads a single file.
     *
     * @param response The HTTP response.
     * @param status   The download status.
     * @param file     The file to download.
     * @param range    The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadFile(HttpServletResponse response, TransferStatus status, File file, LongRange range) throws IOException {
        LOG.info("Starting to download '" + FileUtil.getShortPath(file) + "' to " + status.getPlayer());
        status.setFile(file);

        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodeAsRFC5987(file.getName()));
        if (range == null) {
            Util.setContentLength(response, file.length());
        }

        copyFileToStream(file, RangeOutputStream.wrap(response.getOutputStream(), range), status, range);
        LOG.info("Downloaded '" + FileUtil.getShortPath(file) + "' to " + status.getPlayer());
    }

    private String encodeAsRFC5987(String string) throws UnsupportedEncodingException {
        byte[] stringAsByteArray = string.getBytes("UTF-8");
        char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] attrChar = {'!', '#', '$', '&', '+', '-', '.', '^', '_', '`', '|', '~', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        StringBuilder sb = new StringBuilder();
        for (byte b : stringAsByteArray) {
            if (Arrays.binarySearch(attrChar, b) >= 0) {
                sb.append((char) b);
            } else {
                sb.append('%');
                sb.append(digits[0x0f & (b >>> 4)]);
                sb.append(digits[b & 0x0f]);
            }
        }
        return sb.toString();
    }

    /**
     * Downloads the given files.  The files are packed together in an
     * uncompressed zip-file.
     *
     * @param response    The HTTP response.
     * @param status      The download status.
     * @param files       The files to download.
     * @param indexes     Only download songs at these indexes. May be <code>null</code>.
     * @param range       The byte range, may be <code>null</code>.
     * @param zipFileName The name of the resulting zip file.
     * @throws IOException If an I/O error occurs.
     */
    private void downloadFiles(HttpServletResponse response, TransferStatus status, List<MediaFile> files, int[] indexes, LongRange range, String zipFileName) throws IOException {
        if (indexes != null && indexes.length == 1) {
            downloadFile(response, status, files.get(indexes[0]).getFile(), range);
            return;
        }

        LOG.info("Starting to download '" + zipFileName + "' to " + status.getPlayer());
        response.setContentType("application/x-download");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodeAsRFC5987(zipFileName));

        ZipOutputStream out = new ZipOutputStream(RangeOutputStream.wrap(response.getOutputStream(), range));
        out.setMethod(ZipOutputStream.STORED);  // No compression.

        List<MediaFile> filesToDownload = new ArrayList<MediaFile>();
        if (indexes == null) {
            filesToDownload.addAll(files);
        } else {
            for (int index : indexes) {
                try {
                    filesToDownload.add(files.get(index));
                } catch (IndexOutOfBoundsException x) { /* Ignored */}
            }
        }

        for (MediaFile mediaFile : filesToDownload) {
            zip(out, mediaFile.getParentFile(), mediaFile.getFile(), status, range);
        }

        out.close();
        LOG.info("Downloaded '" + zipFileName + "' to " + status.getPlayer());
    }

    /**
     * Utility method for writing the content of a given file to a given output stream.
     *
     * @param file   The file to copy.
     * @param out    The output stream to write to.
     * @param status The download status.
     * @param range  The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void copyFileToStream(File file, OutputStream out, TransferStatus status, LongRange range) throws IOException {
        LOG.info("Downloading '" + FileUtil.getShortPath(file) + "' to " + status.getPlayer());

        final int bufferSize = 16 * 1024; // 16 Kbit
        InputStream in = new BufferedInputStream(new FileInputStream(file), bufferSize);

        try {
            byte[] buf = new byte[bufferSize];
            long bitrateLimit = 0;
            long lastLimitCheck = 0;

            while (true) {
                long before = System.currentTimeMillis();
                int n = in.read(buf);
                if (n == -1) {
                    break;
                }
                out.write(buf, 0, n);

                // Don't sleep if outside range.
                if (range != null && !range.containsLong(status.getBytesSkipped() + status.getBytesTransfered())) {
                    status.addBytesSkipped(n);
                    continue;
                }

                status.addBytesTransfered(n);
                long after = System.currentTimeMillis();

                // Calculate bitrate limit every 5 seconds.
                if (after - lastLimitCheck > 5000) {
                    bitrateLimit = 1024L * settingsService.getDownloadBitrateLimit() /
                            Math.max(1, statusService.getAllDownloadStatuses().size());
                    lastLimitCheck = after;
                }

                // Sleep for a while to throttle bitrate.
                if (bitrateLimit != 0) {
                    long sleepTime = 8L * 1000 * bufferSize / bitrateLimit - (after - before);
                    if (sleepTime > 0L) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (Exception x) {
                            LOG.warn("Failed to sleep.", x);
                        }
                    }
                }
            }
        } finally {
            out.flush();
            IOUtils.closeQuietly(in);
        }
    }

    /**
     * Writes a file or a directory structure to a zip output stream. File entries in the zip file are relative
     * to the given root.
     *
     * @param out    The zip output stream.
     * @param root   The root of the directory structure.  Used to create path information in the zip file.
     * @param file   The file or directory to zip.
     * @param status The download status.
     * @param range  The byte range, may be <code>null</code>.
     * @throws IOException If an I/O error occurs.
     */
    private void zip(ZipOutputStream out, File root, File file, TransferStatus status, LongRange range) throws IOException {

        // Exclude all hidden files starting with a "."
        if (file.getName().startsWith(".")) {
            return;
        }

        String zipName = file.getCanonicalPath().substring(root.getCanonicalPath().length() + 1);

        if (file.isFile()) {
            status.setFile(file);

            ZipEntry zipEntry = new ZipEntry(zipName);
            zipEntry.setSize(file.length());
            zipEntry.setCompressedSize(file.length());
            zipEntry.setCrc(computeCrc(file));

            out.putNextEntry(zipEntry);
            copyFileToStream(file, out, status, range);
            out.closeEntry();

        } else {
            ZipEntry zipEntry = new ZipEntry(zipName + '/');
            zipEntry.setSize(0);
            zipEntry.setCompressedSize(0);
            zipEntry.setCrc(0);

            out.putNextEntry(zipEntry);
            out.closeEntry();

            File[] children = FileUtil.listFiles(file);
            for (File child : children) {
                zip(out, root, child, status, range);
            }
        }
    }

    /**
     * Computes the CRC checksum for the given file.
     *
     * @param file The file to compute checksum for.
     * @return A CRC32 checksum.
     * @throws IOException If an I/O error occurs.
     */
    private long computeCrc(File file) throws IOException {
        CRC32 crc = new CRC32();
        InputStream in = new FileInputStream(file);

        try {

            byte[] buf = new byte[8192];
            int n = in.read(buf);
            while (n != -1) {
                crc.update(buf, 0, n);
                n = in.read(buf);
            }

        } finally {
            in.close();
        }

        return crc.getValue();
    }

    public void setPlayerService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setPlaylistService(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
}
