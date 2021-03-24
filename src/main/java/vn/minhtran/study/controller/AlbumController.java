package vn.minhtran.study.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import vn.minhtran.study.service.AlbumService;
import vn.minhtran.study.service.MediaService;
import vn.minhtran.study.service.impl.AlbumStatus;

@RestController
@RequestMapping("albums")
public class AlbumController {

	private static Logger LOGGER = LoggerFactory
	        .getLogger(AlbumController.class);

	@Autowired
	private AlbumService albumService;

	@Autowired
	private MediaService mediaService;

	@Autowired
	private ThreadPoolTaskExecutor mediaDownloadExecutor;

	@GetMapping("/download")
	public String downloadAlbums(Authentication authentication,
	        @RequestParam(name = "limit", required = false, defaultValue = "-1") int limit)
	        throws IOException {
		if (limit == 0) {
			return null;
		}
		JsonNode albums = albumService.list();
		JsonNode albumsCon = albums.findValue("albums");
		if (albumsCon.isArray()) {
			int count = 0;
			for (JsonNode al : albumsCon) {
				JsonNode idValueNode = al.findValue("id");
				if (idValueNode != null && !idValueNode.isEmpty()) {
					String albumId = idValueNode.textValue();
					JsonNode albumTitleNode = al.findValue("title");
					String albumTitle = albumTitleNode.textValue();

					downloadAlbum(albumId, albumTitle);
				}
				count++;
				if (limit > 0 && count >= limit) {
					break;
				}
			}
		}

		return null;
	}

	@GetMapping("/list")
	public JsonNode listAlbums(Authentication authentication)
	        throws IOException {
		LOGGER.info("Listing....");
		return albumService.list();
	}

	private void downloadAlbum(String albumId, String albumTitle) {
		if (shouldDownloadAlbum(albumId, albumTitle)) {

			LOGGER.info("Reading album {}...", albumTitle);
			try {
				JsonNode albumContent = albumService.albumContent(albumId);
				JsonNode mediaItemsCon = albumContent.findValue("mediaItems");
				if (mediaItemsCon.isArray()) {
					int size = mediaItemsCon.size();
					albumService.addAlbum(albumId, albumTitle, size);
					for (JsonNode mcj : mediaItemsCon) {
						String filename = mcj.findValue("filename").textValue();
						JsonNode mediaMetadata = mcj
						        .findParent("mediaMetadata");
						String width = mediaMetadata.findValue("width")
						        .textValue();
						String height = mediaMetadata.findValue("height")
						        .textValue();
						final String baseUrl = String.format("%s=w%s-h%s",
						        mcj.findValue("baseUrl").textValue(), width,
						        height);
						mediaDownloadExecutor.execute(() -> {
							LOGGER.info("Download file [{}]...", filename);
							mediaService.downloadPhoto(baseUrl, albumId,
							        albumTitle, width, height, filename);
						});
					}
				}
			} catch (Exception e) {
				LOGGER.error("Error when process album [{}]", albumTitle, e);
			}
		}
	}

	private boolean shouldDownloadAlbum(String albumId, String albumTitle) {
		AlbumStatus status = albumService.albumLocalStatus(albumId);
		return status == null || status == AlbumStatus.DOWNLOADING ? true
		        : false;
	}

}
