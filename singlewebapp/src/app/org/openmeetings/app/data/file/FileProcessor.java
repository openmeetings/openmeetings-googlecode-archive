package org.openmeetings.app.data.file;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openmeetings.app.data.file.dao.FileExplorerItemDaoImpl;
import org.openmeetings.app.data.flvrecord.converter.FlvExplorerConverter;
import org.openmeetings.app.documents.GenerateImage;
import org.openmeetings.app.documents.GeneratePDF;
import org.openmeetings.app.documents.GenerateThumbs;
import org.openmeetings.app.persistence.beans.files.FileExplorerItem;
import org.openmeetings.app.remote.red5.ScopeApplicationAdapter;
import org.openmeetings.utils.StoredFile;
import org.openmeetings.utils.crypt.MD5;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public class FileProcessor {

	private static final Logger log = Red5LoggerFactory.getLogger(FileProcessor.class, ScopeApplicationAdapter.webAppRootKey);

	//Spring loaded Beans
	@Autowired
	private FlvExplorerConverter flvExplorerConverter;
	@Autowired
	private FileExplorerItemDaoImpl fileExplorerItemDao;
	@Autowired
	private GenerateImage generateImage;
	@Autowired
	private GenerateThumbs generateThumbs;
	@Autowired
	private GeneratePDF generatePDF;

	private String workingDir = "";
	private String working_dirppt = "";

	private void prepareFolderStructure(String current_dir) throws Exception {
		
		this.workingDir = current_dir + "upload" + File.separatorChar
        						+ "files" + File.separatorChar;

		// System.out.println("IS SYSTEM PROFILE");
		// Add the Folder for the Room if it does not exist yet
		File localFolder = new File(workingDir);
		if (!localFolder.exists()) {
		    localFolder.mkdir();
		}
		
		this.working_dirppt = current_dir + "uploadtemp" + File.separatorChar
		        + "files" + File.separatorChar;
		
		// add Temp Folder Structure
		File localFolderppt = new File(working_dirppt);
		if (!localFolderppt.exists()) {
		    localFolderppt.mkdir();
		}
		
		log.debug("this.workingDir: " + this.workingDir);
		
	}
	
	public HashMap<String, HashMap<String, String>> processFile(Long userId, Long room_id, 
			boolean isOwner, InputStream is, Long parentFolderId, String fileSystemName, 
			String current_dir, Map<String, Object> hs, Long externalFileId, 
			String externalType) throws Exception {
		
		HashMap<String, HashMap<String, String>> returnError = new HashMap<String, HashMap<String, String>>();
		
		HashMap<String, String> returnAttributes = new HashMap<String, String>();
        returnAttributes.put("process", "");
        returnAttributes.put("command", "");
        returnAttributes.put("exitValue", "0");
        returnAttributes.put("error","");
		
		//prepare the necessary folders
		this.prepareFolderStructure(current_dir);
		
		int dotidx = fileSystemName.lastIndexOf('.');

        // Generate a random string to prevent any problems with
        // foreign characters and duplicates
        Date d = new Date();
        String newFileSystemName = MD5.do_checksum("FILE_" + d.getTime());

        String newFileExtDot = fileSystemName.substring(dotidx, fileSystemName.length()).toLowerCase();
        String newFileExt = newFileExtDot.substring(1);
        log.debug("newFileExt: " + newFileExt);
        StoredFile storedFile = new StoredFile(newFileSystemName, newFileExt); 

        // Check variable to see if this file is a presentation
        // check if this is a a file that can be converted by
        // openoffice-service
        boolean canBeConverted = storedFile.isConvertable();
        boolean isPdf = storedFile.isPdf();
        boolean isImage = storedFile.isImage();
        boolean isChart = storedFile.isChart();
        boolean isAsIs = storedFile.isAsIs();
        boolean isVideo = storedFile.isVideo();

        String completeName = "";
        log.debug("isAsIs: " + isAsIs);

        // add outputfolders for profiles
        // if it is a presenation it will be copied to another place
        if (!(canBeConverted || isPdf || isImage || isVideo || isAsIs)) {
        	returnAttributes.put("error","The file type cannot be converted");
        	returnAttributes.put("exitValue", "-1");
            return returnError;
        }

        if (isAsIs) {
            // check if this is a room file or UserProfile
            completeName = workingDir + newFileSystemName;
        } else {
            completeName = working_dirppt + newFileSystemName;
        }
        log.debug("writing file to: " + completeName + newFileExtDot);
        
        returnAttributes.put("completeName", completeName);

        FileOutputStream fos = new FileOutputStream(completeName + newFileExtDot);
        byte[] buffer = new byte[1024];
        int len = 0;

        while (len != (-1)) {
            len = is.read(buffer, 0, 1024);
            if (len != (-1))
                fos.write(buffer, 0, len);
        }

        fos.close();
        is.close();

        Long ownerId = null;
        if (parentFolderId == -2) {
            parentFolderId = 0L;
            ownerId = userId;
        }
        if (isOwner) {
            ownerId = userId;
        }

        String fileHashName = newFileSystemName + newFileExtDot;
        Boolean isPresentation = false;
        if (canBeConverted || isPdf) {
            // In case of a presentation the hash is a folder-name
            fileHashName = newFileSystemName;
            isPresentation = true;
        }
        if (isImage) {
            fileHashName = newFileSystemName + ".jpg";
        }
        if (isVideo) {
            fileHashName = newFileSystemName + ".flv";
        }

        FileExplorerItem fileExplorerItem = fileExplorerItemDao.getFileExplorerItemsById(parentFolderId);

        if (fileExplorerItem != null) {
            if (fileExplorerItem.getIsFolder() == null
                    || !fileExplorerItem.getIsFolder()) {
                parentFolderId = 0L;
            }
        }

        Long fileExplorerItemId = fileExplorerItemDao.add(
                fileSystemName, fileHashName, // The Hashname of the file
                parentFolderId, ownerId, room_id, userId, false, // isFolder
                isImage, isPresentation, "", false, isChart, 
                externalFileId, externalType);
        log.debug("fileExplorerItemId: " + fileExplorerItemId);
        
        
        returnAttributes.put("fileExplorerItemId", "" + fileExplorerItemId);
        
        log.debug("canBeConverted: " + canBeConverted);
        if (canBeConverted) {
            // convert to pdf, thumbs, swf and xml-description
            returnError = generatePDF.convertPDF(current_dir,
                    newFileSystemName, newFileExtDot, "files", true,
                    completeName);
        } else if (isPdf) {
            // convert to thumbs, swf and xml-description
            returnError = generatePDF.convertPDF(current_dir,
                    newFileSystemName, newFileExtDot, "files", false,
                    completeName);
        } else if (isChart) {
            log.debug("uploaded chart file");
        } else if (isImage && !isAsIs) {
            // convert it to JPG
            log.debug("##### convert it to JPG: ");
            returnError = generateImage.convertImage(current_dir,
                    newFileSystemName, newFileExtDot, "files",
                    newFileSystemName, false);
        } else if (isAsIs) {
            HashMap<String, String> processThumb = generateThumbs.generateThumb("_thumb_", current_dir, completeName, 50);
            returnError.put("processThumb", processThumb);
        } else if (isVideo) {
             flvExplorerConverter.startConversion(fileExplorerItemId, completeName + newFileExtDot);
        }
		
        returnError.put("returnAttributes", returnAttributes);
        
		return returnError;
		
	}
	
}
