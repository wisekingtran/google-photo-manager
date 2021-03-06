package vn.minhtran.study.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import vn.minhtran.study.service.impl.AlbumStatus;

public interface AlbumService {

	ArrayNode list();

	ArrayNode listAlbumMedia(String albumId) throws Exception;

	AlbumStatus getAlbumStatus(String albumId);

	void addAlbum(String albumId, String albumTitle, int size);

	int getAlbumSize(String albumId);

	void downloadComplete(String albumId);

	ArrayNode listAlbum(AlbumStatus... statuses);

	ObjectNode getAlbum(String albumId);

}
