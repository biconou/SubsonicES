<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="iso-8859-1"%>
<%--
  ~ This file is part of Subsonic.
  ~
  ~  Subsonic is free software: you can redistribute it and/or modify
  ~  it under the terms of the GNU General Public License as published by
  ~  the Free Software Foundation, either version 3 of the License, or
  ~  (at your option) any later version.
  ~
  ~  Subsonic is distributed in the hope that it will be useful,
  ~  but WITHOUT ANY WARRANTY; without even the implied warranty of
  ~  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  ~  GNU General Public License for more details.
  ~
  ~  You should have received a copy of the GNU General Public License
  ~  along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.
  ~
  ~  Copyright 2014 (C) Sindre Mehus
  --%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%--@elvariable id="model" type="java.util.Map"--%>

<html><head>
    <%@ include file="head.jsp" %>
    <%@ include file="jquery.jsp" %>
    <script type="text/javascript" src="<c:url value="/script/scripts-2.0.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/engine.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/starService.js"/>"></script>
    <script type="text/javascript" src="<c:url value="/dwr/interface/multiService.js"/>"></script>

</head><body class="mainframe bgcolor1" onload="init();">

<script type="text/javascript" language="javascript">
    function init() {
        <c:if test="${model.showArtistInfo}">
        loadArtistInfo();
        </c:if>
    }

    function loadArtistInfo() {
        multiService.getArtistInfo(${model.dir.id}, 8, function (artistInfo) {
            if (artistInfo.similarArtists.length > 0) {
                var html = "";
                for (var i = 0; i < artistInfo.similarArtists.length; i++) {
                    html += "<a href='main.view?id=" + artistInfo.similarArtists[i].mediaFileId + "' target='main'>" +
                            escapeHtml(artistInfo.similarArtists[i].artistName) + "</a>";
                    if (i < artistInfo.similarArtists.length - 1) {
                        html += " <span class='similar-artist-divider'>|</span> ";
                    }
                }
                $("#similarArtists").append(html);
                $("#similarArtists").show();
                $("#similarArtistsTitle").show();
                $("#similarArtistsRadio").show();
            }

            if (artistInfo.artistBio && artistInfo.artistBio.biography) {
                $("#artistBio").append(artistInfo.artistBio.biography);
                if (artistInfo.artistBio.mediumImageUrl) {
                    $("#artistImage").attr("src", artistInfo.artistBio.mediumImageUrl);
                    $("#artistImage").show();
                }
            }
        });
    }

    function toggleStar(mediaFileId, imageId) {
        if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOnImage"/>") != -1) {
            $(imageId).attr("src", "<spring:theme code="ratingOffImage"/>");
            starService.unstar(mediaFileId);
        }
        else if ($(imageId).attr("src").indexOf("<spring:theme code="ratingOffImage"/>") != -1) {
            $(imageId).attr("src", "<spring:theme code="ratingOnImage"/>");
            starService.star(mediaFileId);
        }
    }

    function playAll() {
        top.playQueue.onPlay(${model.dir.id});
    }

    function playRandom() {
        top.playQueue.onPlayRandom(${model.dir.id}, 40);
    }

    function addAll() {
        top.playQueue.onAdd(${model.dir.id});
    }

    function playSimilar() {
        top.playQueue.onPlaySimilar(${model.dir.id}, 50);
    }

</script>

<div style="float:left">
    <h1>
        <img id="starImage" src="<spring:theme code="${not empty model.dir.starredDate ? 'ratingOnImage' : 'ratingOffImage'}"/>"
             onclick="toggleStar(${model.dir.id}, '#starImage'); return false;" style="cursor:pointer" alt="">

        <span style="vertical-align: middle">
            <c:forEach items="${model.ancestors}" var="ancestor">
                <sub:url value="main.view" var="ancestorUrl">
                    <sub:param name="id" value="${ancestor.id}"/>
                </sub:url>
                <a href="${ancestorUrl}">${fn:escapeXml(ancestor.name)}</a> &raquo;
            </c:forEach>
            ${fn:escapeXml(model.dir.name)}
        </span>
    </h1>

    <c:if test="${not model.partyMode}">
        <h2>
            <c:if test="${model.navigateUpAllowed}">
                <sub:url value="main.view" var="upUrl">
                    <sub:param name="id" value="${model.parent.id}"/>
                </sub:url>
                <span class="header"><a href="${upUrl}"><fmt:message key="main.up"/></a></span>
                <c:set var="needSep" value="true"/>
            </c:if>

            <c:if test="${model.user.streamRole}">
                <c:if test="${needSep}">|</c:if>
                <span class="header"><a href="javascript:playAll()"><fmt:message key="main.playall"/></a></span> |
                <span class="header"><a href="javascript:playRandom(0)"><fmt:message key="main.playrandom"/></a></span> |
                <span class="header"><a href="javascript:addAll(0)"><fmt:message key="main.addall"/></a></span>
                <c:set var="needSep" value="true"/>
            </c:if>

            <c:if test="${model.user.commentRole}">
                <c:if test="${needSep}">|</c:if>
                <span class="header"><a href="javascript:toggleComment()"><fmt:message key="main.comment"/></a></span>
            </c:if>
        </h2>
    </c:if>
</div>

<%@ include file="viewSelector.jsp" %>
<div style="clear:both"></div>

<div id="comment" class="albumComment"><sub:wiki text="${model.dir.comment}"/></div>

<div id="commentForm" style="display:none">
    <form method="post" action="setMusicFileInfo.view">
        <input type="hidden" name="action" value="comment">
        <input type="hidden" name="id" value="${model.dir.id}">
        <textarea name="comment" rows="6" cols="70">${model.dir.comment}</textarea>
        <input type="submit" value="<fmt:message key="common.save"/>">
    </form>
    <fmt:message key="main.wiki"/>
</div>

<script type='text/javascript'>
    function toggleComment() {
        $("#commentForm").toggle();
        $("#comment").toggle();
    }
</script>

<c:choose>
    <c:when test="${model.viewAsList}">
        <table class="music indent">
            <c:forEach items="${model.subDirs}" var="subDir">
                <tr>
                    <c:import url="playButtons.jsp">
                        <c:param name="id" value="${subDir.id}"/>
                        <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                        <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                        <c:param name="asTable" value="true"/>
                    </c:import>
                    <td class="truncate"><a href="main.view?id=${subDir.id}" title="${fn:escapeXml(subDir.name)}">${fn:escapeXml(subDir.name)}</a></td>
                    <td class="fit rightalign detail">${subDir.year}</td>
                </tr>
            </c:forEach>
        </table>
    </c:when>

    <c:otherwise>
        <table class="music indent">
            <c:forEach items="${model.subDirs}" var="subDir">
                <c:if test="${not subDir.album}">
                    <tr>
                        <c:import url="playButtons.jsp">
                            <c:param name="id" value="${subDir.id}"/>
                            <c:param name="playEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="addEnabled" value="${model.user.streamRole and not model.partyModeEnabled}"/>
                            <c:param name="asTable" value="true"/>
                        </c:import>
                        <td class="truncate"><a href="main.view?id=${subDir.id}" title="${fn:escapeXml(subDir.name)}">${fn:escapeXml(subDir.name)}</a></td>
                        <td class="fit rightalign detail">${subDir.year}</td>
                    </tr>
                </c:if>
            </c:forEach>
        </table>

        <div style="float: left;padding-top: 1.5em">
            <c:forEach items="${model.subDirs}" var="subDir" varStatus="loopStatus">
                <c:if test="${subDir.album}">
                    <div class="albumThumb">
                        <c:import url="coverArt.jsp">
                            <c:param name="albumId" value="${subDir.id}"/>
                            <c:param name="caption1" value="${fn:escapeXml(subDir.name)}"/>
                            <c:param name="caption2" value="${subDir.year}"/>
                            <c:param name="captionCount" value="2"/>
                            <c:param name="coverArtSize" value="${model.coverArtSizeMedium}"/>
                            <c:param name="showLink" value="true"/>
                            <c:param name="appearAfter" value="${loopStatus.count * 30}"/>
                        </c:import>
                    </div>
                </c:if>
            </c:forEach>
        </div>
    </c:otherwise>
</c:choose>

<table style="width:90%;padding-top:2em;clear:both">
    <tr>
        <td rowspan="4" style="vertical-align: top">
            <img id="artistImage" class="dropshadow" alt="" style="margin-right: 1em; display: none">
        </td>
        <td id="artistBio" style="padding-bottom: 0.5em"></td>
    </tr>
    <tr><td style="padding-bottom: 0.5em">
        <span id="similarArtistsTitle" style="padding-right: 0.5em; display: none"><fmt:message key="main.similarartists"/>:</span>
        <span id="similarArtists"></span>
    </td></tr>
    <tr><td>
        <div id="similarArtistsRadio" class="forward" style="display: none">
            <a href="javascript:playSimilar()"><fmt:message key="main.startradio"/></a>
        </div>
    </td></tr>
    <tr><td style="height: 100%"></td></tr>
</table>

</body>
</html>
