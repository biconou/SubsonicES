<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1" %>

<html>
<head>
    <%@ include file="head.jsp" %>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/util.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/chatService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/nowPlayingService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/prototype.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/script/scripts-2.0.js"/>"></script>
</head>
<body class="bgcolor1 rightframe" style="padding-top:2em" onload="init()">

<script type="text/javascript">
    function init() {
        dwr.engine.setErrorHandler(null);
    <c:if test="${model.showChat}">
        chatService.addMessage(null);
    </c:if>
    }
</script>

<c:if test="${not model.licenseInfo.licenseValid}">
    <div class="detail" style="text-align: center;padding-bottom: 1em">
        <a href="premium.view" target="main"><img src="<spring:theme code="donateSmallImage"/>" alt="">
            <fmt:message key="top.getpremium"/></a>
        <c:if test="${model.licenseInfo.trialDaysLeft gt 0}">
            <br>
            <a href="premium.view" target="main"><fmt:message key="top.trialdaysleft"><fmt:param value="${model.licenseInfo.trialDaysLeft}"/></fmt:message></a>
        </c:if>
    </div>
</c:if>

<c:if test="${model.newVersionAvailable}">
    <div class="warning" style="padding-bottom: 1em">
        <fmt:message key="top.upgrade"><fmt:param value="${model.brand}"/><fmt:param value="${model.latestVersion}"/></fmt:message>
    </div>
</c:if>

<div id="scanningStatus" style="display: none;" class="warning">
    <img src="<spring:theme code="scanningImage"/>" title="" alt=""> <fmt:message key="main.scanning"/> <span id="scanCount"></span>
</div>

<c:if test="${model.showNowPlaying}">

    <!-- This script uses AJAX to periodically retrieve what all users are playing. -->
    <script type="text/javascript" language="javascript">

        startGetNowPlayingTimer();

        function startGetNowPlayingTimer() {
            nowPlayingService.getNowPlaying(getNowPlayingCallback);
            setTimeout("startGetNowPlayingTimer()", 10000);
        }

        function getNowPlayingCallback(nowPlaying) {
            var html = nowPlaying.length == 0 ? "" : "<h2><fmt:message key="main.nowplaying"/></h2><table style='width:100%'>";
            for (var i = 0; i < nowPlaying.length; i++) {
                html += "<tr><td colspan='2' class='detail' style='padding-top:1em;white-space:nowrap'>";

                if (nowPlaying[i].avatarUrl != null) {
                    html += "<img src='" + nowPlaying[i].avatarUrl + "' style='padding-right:5pt;width:30px;height:30px'>";
                }
                html += "<b>" + nowPlaying[i].username + "</b></td></tr>";

                html += "<tr><td class='detail' style='padding-right:1em'>" +
                        "<a title='" + nowPlaying[i].tooltip + "' target='main' href='" + nowPlaying[i].albumUrl + "'>";

                if (nowPlaying[i].artist != null) {
                    html += nowPlaying[i].artist + "<br/>";
                }

                html += "<span class='songTitle'>" + nowPlaying[i].title + "</span></a><br/>";
                if (nowPlaying[i].lyricsUrl != null) {
                    html += "<span class='forward'><a href='" + nowPlaying[i].lyricsUrl + "' onclick=\"return popupSize(this, 'lyrics', 430, 550)\">" +
                            "<fmt:message key="main.lyrics"/>" + "</a></span>";
                }
                html += "</td><td>" +
                        "<a title='" + nowPlaying[i].tooltip + "' target='main' href='" + nowPlaying[i].albumUrl + "'>" +
                        "<img src='" + nowPlaying[i].coverArtUrl + "' class='dropshadow' height='60' width='60'></a>" +
                        "</td></tr>";

                var minutesAgo = nowPlaying[i].minutesAgo;
                if (minutesAgo > 4) {
                    html += "<tr><td class='detail' colspan='2'>" + minutesAgo + " <fmt:message key="main.minutesago"/></td></tr>";
                }
            }
            html += "</table>";
            $('nowPlaying').innerHTML = html;
        }
    </script>

    <div id="nowPlaying">
    </div>

</c:if>

<c:if test="${model.showChat}">
    <script type="text/javascript">

        var revision = 0;
        startGetMessagesTimer();

        function startGetMessagesTimer() {
            chatService.getMessages(revision, getMessagesCallback);
            setTimeout("startGetMessagesTimer()", 10000);
        }

        function addMessage() {
            chatService.addMessage($("message").value);
            dwr.util.setValue("message", null);
            setTimeout("startGetMessagesTimer()", 500);
        }
        function clearMessages() {
            chatService.clearMessages();
            setTimeout("startGetMessagesTimer()", 500);
        }
        function getMessagesCallback(messages) {

            if (messages == null) {
                return;
            }
            revision = messages.revision;

            // Delete all the rows except for the "pattern" row
            dwr.util.removeAllRows("chatlog", { filter:function(div) {
                return (div.id != "pattern");
            }});

            // Create a new set cloned from the pattern row
            for (var i = 0; i < messages.messages.length; i++) {
                var message = messages.messages[i];
                var id = i + 1;
                dwr.util.cloneNode("pattern", { idSuffix:id });
                dwr.util.setValue("user" + id, message.username);
                dwr.util.setValue("date" + id, " [" + formatDate(message.date) + "]");
                dwr.util.setValue("content" + id, message.content);
                $("pattern" + id).show();
            }

            var clearDiv = $("clearDiv");
            if (clearDiv) {
                if (messages.messages.length == 0) {
                    clearDiv.hide();
                } else {
                    clearDiv.show();
                }
            }
        }
        function formatDate(date) {
            var hours = date.getHours();
            var minutes = date.getMinutes();
            var result = hours < 10 ? "0" : "";
            result += hours;
            result += ":";
            if (minutes < 10) {
                result += "0";
            }
            result += minutes;
            return result;
        }
    </script>

    <script type="text/javascript">

        startGetScanningStatusTimer();

        function startGetScanningStatusTimer() {
            nowPlayingService.getScanningStatus(getScanningStatusCallback);
        }

        function getScanningStatusCallback(scanInfo) {
            dwr.util.setValue("scanCount", scanInfo.count);
            if (scanInfo.scanning) {
                $("scanningStatus").show();
                setTimeout("startGetScanningStatusTimer()", 1000);
            } else {
                $("scanningStatus").hide();
                setTimeout("startGetScanningStatusTimer()", 15000);
            }
        }
    </script>

    <h2><fmt:message key="main.chat"/></h2>
    <div style="padding-top:0.3em;padding-bottom:0.3em">
        <input type="text" id="message" value=" <fmt:message key="main.message"/>" style="width:100%" onclick="dwr.util.setValue('message', null);" onkeypress="dwr.util.onReturn(event, addMessage)"/>
    </div>

    <table>
        <tbody id="chatlog">
        <tr id="pattern" style="display:none;margin:0;padding:0 0 0.15em 0;border:0"><td>
            <span id="user" class="detail" style="font-weight:bold"></span>&nbsp;<span id="date" class="detail"></span> <span id="content"></span></td>
        </tr>
        </tbody>
    </table>

    <c:if test="${model.user.adminRole}">
        <div id="clearDiv" style="display:none;" class="forward"><a href="javascript:clearMessages()"> <fmt:message key="main.clearchat"/></a></div>
    </c:if>
</c:if>

</body>
</html>
