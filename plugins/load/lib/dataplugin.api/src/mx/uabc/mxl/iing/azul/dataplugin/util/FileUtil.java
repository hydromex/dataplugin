package mx.uabc.mxl.iing.azul.dataplugin.util;
/*
    Copyright (C) 2017  Jesús Donaldo Osornio Hernández
    Copyright (C) 2017  Luis Alejandro Herrera León
    Copyright (C) 2017  Gabriel Alejandro López Morteo

    This file is part of DataPlugin.

    DataPlugin is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataPlugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with DataPlugin.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import mx.uabc.mxl.iing.azul.dataplugin.logger.MessageMediator;


/**
 * Utility class for working with files, URLs and streams
 *
 * @author jdosornio
 * @version %I%
 */
public class FileUtil {
	
	/**
	 * Copies all the specified url files to the given directory
	 * 
	 * @param dir the directory where the files will be copied
	 * @param files a url file or files to be copied to the specified directory
	 * 
	 * @return an array of the files copied to the directory or null in case of
	 * error
	 */
	public static File[] copyFilesTo(File dir, URL ... files) {
		List<File> copiedFiles = null;
		
    	if (dir != null && dir.isDirectory()) {
    		copiedFiles = new ArrayList<>();
    		
    		//Copy all url files
			for (URL urlFile : files) {
				//Get the url file name
				String fileName = getFileName(urlFile);
				
				if (fileName != null && !fileName.isEmpty()) {
					//Create destination file for the directory
					File destFile = new File(dir, fileName);
	
					// Copy file
					copyURLToFile(urlFile, destFile);
					copiedFiles.add(destFile);
				}
			}
    	} else {
    		MessageMediator.sendMessage("Failed to copy script files to directory: [" +
    				dir + "]",
    				MessageMediator.ERROR_MESSAGE);
    	}
    	
    	return (copiedFiles != null) ? copiedFiles
				.toArray(new File[copiedFiles.size()]) : null;
    }
	
	/**
	 * Get the file name of this url file.
	 * 
	 * @param urlFile the url of the file. Must be pointing to a true file (file protocol or
	 * related).
	 * 
	 * @return the file name of this url file or null if some error occurs.
	 */
	private static String getFileName(URL urlFile) {
		String fileName = null;
		
		String separator = File.separator;
		//Another backslash for the regex to work in Windows, although this
		//script works only in Linux
		if (separator.equals("\\")) {
			separator = "\\\\";
		}
		
		if (urlFile != null && (urlFile.getProtocol().equals("file")
				|| urlFile.getFile().startsWith("file"))) {
			//get file name
			fileName = urlFile.getFile().replaceAll(".*" + separator, "");
		} else {
			MessageMediator.sendMessage("Failed to get name of url: [" +
    				urlFile + "]", MessageMediator.ERROR_MESSAGE);
		}
		
		return fileName;
	}
	
	/**
	 * Copies the specified url file to the specified destination file.
	 * 
	 * @param urlSource the url of the file to be copied
	 * @param dest the destination path where the url file will be copied
	 */
	private static void copyURLToFile(URL urlSource, File dest) {
		//Open inputStream to urlSource and copy it to the specified file
		
		if (urlSource != null && dest != null && !dest.isDirectory()) {
			InputStream srcStream = null;
			
			try {
				//Copy source to file
				srcStream = urlSource.openStream();
				Files.copy(srcStream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
			
			} catch(IOException ex) {
				MessageMediator.sendMessage(ex.toString(), MessageMediator.ERROR_MESSAGE);
				//remove file
				deleteFiles(dest);
				
			} finally {
				//free input stream
				if (srcStream != null) {
					try {
						srcStream.close();
					} catch (IOException e) {
						MessageMediator.sendMessage(e.toString(), MessageMediator.ERROR_MESSAGE);
					}
				}
			}
		}
		else {
			MessageMediator.sendMessage("Failed to copy url: [" + urlSource + "] to file: [" +
    				dest + "]", MessageMediator.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Deletes all files specified, only if they exist already in the filesystem
	 * 
	 * @param files a file or files to be deleted
	 * 
	 * @return true if all files where deleted, false otherwise.
	 */
	public static boolean deleteFiles(File ... files) {
		int ok = 1;
		
		if (files != null && files.length > 0) {
			ok = files.length;
			
			for (File file : files) {
				
				try {
					ok -= (Files.deleteIfExists(file.toPath()) ? 1 : 0);
				} catch (IOException e) {
					MessageMediator.sendMessage(e.toString(), MessageMediator.ERROR_MESSAGE);
				}
			}
		}
		else {
			MessageMediator.sendMessage("deleteFiles: files argument is empty",
					MessageMediator.INFO_MESSAGE);
		}
		
		return (ok == 0);
	}

    /**
     * Deletes the given directory
     *
     * @param dir a file object representing the directory to delete
     *
     * @return true if deleted, false otherwise
     */
	public static boolean deleteDir(File dir) {
        boolean ok = false;

        if (dir != null && dir.isDirectory()) {
//            deleteFiles(dir.listFiles());
//            ok = dir.delete();
            try {
                Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException ex) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                ok = true;
            } catch (IOException e) {
                MessageMediator.sendMessage(e.toString(), MessageMediator.ERROR_MESSAGE);
            }
        }

        return ok;
    }

    /**
     * Utility method to transform an input stream to a String object.
     *
     * @param is the input stream
     *
     * @return a string with the contents of the input stream (must fit into memory) or null, in case is = null
     *
     * @throws IOException in case the input stream couldn't be read
     */
	public static String streamToString(InputStream is) throws IOException {
        if(is == null) {
            MessageMediator.sendMessage("InputStream is null", MessageMediator.ERROR_MESSAGE);
            return null;
        }

		Scanner s = new Scanner(is).useDelimiter("\\A");
        String content = s.hasNext() ? s.next() : "";

        is.close();

        return content;
	}
}