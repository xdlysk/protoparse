package com.xdlysk.protoparse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.xdlysk.protoparse.Parser;

public class app {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//String descriptorPath = args[0];
		String savePath = null;
		if(args.length>1){
			savePath = args[1];
		}
		String descriptorPath = "D:\\HackToolbox\\protoc-2.5.0-win32\\test.txt";
		String descriptorString = getProtoDescriptor(descriptorPath);
		if(descriptorString==null){
			System.err.println("descriptorString==null");
			return;
		}
		
		if(savePath==null){
			savePath = System.getProperty("user.dir");
		}
		
		saveToFile(descriptorString,savePath);
	}
	
	//write message file
	private static void saveToFile(String descriptorString,String savePath) {
		Parser parser = new Parser(descriptorString);
		String s = parser.builderMessageBody();
		try {
			String encoding="ISO-8859-1";
	        File file=new File(savePath + File.separator + parser.getName());
	        if(!file.exists()){ //判断文件是否存在
	        	file.createNewFile();
	        }
	        OutputStreamWriter write = new OutputStreamWriter(new FileOutputStream(file),encoding);//考虑到编码格式
            BufferedWriter bufferedWriter = new BufferedWriter(write);
            bufferedWriter.write(s);
            bufferedWriter.flush();
            write.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String getProtoDescriptor(String fileName){
		try{
			String encoding="ISO-8859-1";
	        File file=new File(fileName);
	        String lineTxt = null;
	        if(file.isFile() && file.exists()){ //判断文件是否存在
	            InputStreamReader read = new InputStreamReader(
	            new FileInputStream(file),encoding);//考虑到编码格式
	            BufferedReader bufferedReader = new BufferedReader(read);
	            lineTxt = bufferedReader.readLine();
	            read.close();
	        }
	        return lineTxt;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
		
	}
}
