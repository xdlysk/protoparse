package com.xdlysk.protoparse;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto.ExtensionRange;
import com.google.protobuf.DescriptorProtos.EnumDescriptorProto;
import com.google.protobuf.DescriptorProtos.EnumValueDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldOptions;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.FileOptions.OptimizeMode;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.UnknownFieldSet.Field;

public class Parser {
	private StringBuilder _sb;
	private FileDescriptorProto _proto;
	private String _indent;
	private static HashMap<Type, String> _typeMap;
	private static HashMap<Label, String> _labelMap;
	
	static int EXTENSIONS_MAX = 536870912;
	
	static {
		_typeMap = new HashMap<Type, String>();
		_typeMap.put(Type.TYPE_INT32, "int32");
		_typeMap.put(Type.TYPE_FIXED32, "fixed32");
		_typeMap.put(Type.TYPE_SFIXED32, "sfixed32");
		_typeMap.put(Type.TYPE_UINT32, "uint32");
		_typeMap.put(Type.TYPE_SINT32, "sint32");
		
		_typeMap.put(Type.TYPE_INT64, "int64");
		_typeMap.put(Type.TYPE_FIXED64, "fixed64");
		_typeMap.put(Type.TYPE_SFIXED64, "sfixed64");
		_typeMap.put(Type.TYPE_UINT64, "uint64");
		_typeMap.put(Type.TYPE_SINT64, "sint64");
		
		_typeMap.put(Type.TYPE_DOUBLE, "double");
		_typeMap.put(Type.TYPE_FLOAT, "float");
		
		_typeMap.put(Type.TYPE_BOOL, "boolean");
		_typeMap.put(Type.TYPE_BYTES, "bytes");
		
		_typeMap.put(Type.TYPE_STRING, "string");
		
		_labelMap = new HashMap<Label, String>();
		_labelMap.put(Label.LABEL_OPTIONAL, "optional");
		_labelMap.put(Label.LABEL_REPEATED, "repeated");
		_labelMap.put(Label.LABEL_REQUIRED, "required");
	}
	
	public Parser(String protostring){
		_indent = "";
		_sb = new StringBuilder();
		_proto = restoreProto(convertPlainText2Literal(protostring));
		if(_proto == null){
			return;
		}
	}
	
	public String getName(){
		return _proto.getName();
	}
	
	private String convertPlainText2Literal(String plain){
		StringBuilder sb = new StringBuilder();
		
		String pattern = "^\\\\((u[0-9a-f]{4})|([0-7]{3})|([abfnrtv'\"\\\\]))";
		Pattern r = Pattern.compile(pattern);
		while(!plain.equals("")){
			Matcher m = r.matcher(plain);
			//找到待转义字符
			if(!m.find()){
				int index=0;
				for(;index<plain.length();index++){
					char cchar = plain.charAt(index);
					if(cchar=='\\'){
						break;
					}else{
						sb.append(cchar);
					}
				}
				plain = plain.substring(index);
			}else{
				int index=2;
				String mr = m.group(0);
				switch (mr) {
				case "\\a":
					sb.append((char)0x07);
					break;
				case "\\b":
					sb.append('\b');
					break;
				case "\\f":
					sb.append('\f');
					break;
				case "\\v":
					sb.append((char)0x0b);
					break;
				case "\\r":
					sb.append('\r');
					break;
				case "\\n":
					sb.append('\n');
					break;
				case "\\t":
					sb.append('\t');
					break;
				case "\\\"":
					sb.append('"');
					break;
				case "\\\\":
					sb.append('\\');
					break;
				case "\\'":
					sb.append('\'');
					break;
				default:
					if(mr.startsWith("\\u")){
						String hex = mr.substring(2);
						sb.append((char)Integer.parseInt(hex,16));
						index = 6;
					}else{
						String oct = mr.substring(1);
						sb.append((char)Integer.parseInt(oct, 8));
						index = 4;
					}
					break;
				}
				plain =plain.substring(index);
			}
			
		}
		return sb.toString();
	}
	
	private FileDescriptorProto restoreProto(String descriptor){
		byte[] descriptorBytes;
		try {
			descriptorBytes = descriptor.getBytes("ISO-8859-1");
			FileDescriptorProto proto = FileDescriptorProto.parseFrom(descriptorBytes);
			return proto;
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
			return null;
		}
		
	}
	
	
	//write line with indent and with \r\n
	private void writeLine(String str) {
		_sb.append(_indent);
		_sb.append(str);
		_sb.append(System.lineSeparator());
	}
	
	private void writeEmptyLine() {
		_sb.append(System.lineSeparator());
	}
	
	public String builderMessageBody() {
		buildDependency();
		buildFilePackage();
		buildOptions();
		
		buildPublicDependency();
		
		List<DescriptorProto> message = _proto.getMessageTypeList();
		for (DescriptorProto descriptorProto : message) {
			buildMessageType(descriptorProto);
		}
		
		List<EnumDescriptorProto> enums = _proto.getEnumTypeList();
		for (EnumDescriptorProto enumDescriptorProto : enums) {
			buildEnumType(enumDescriptorProto);
		}
		
		List<FieldDescriptorProto> exts = _proto.getExtensionList();
		buildExtend(exts);
		
		return _sb.toString();
	}
	
	private void addIndent() {
		_indent+="    ";
	}
	
	private void reduceIndent() {
		_indent = _indent.substring(4);
	}
	
	private void buildDependency() {
		List<String> deplist = _proto.getDependencyList();
		for (String string : deplist) {
			writeLine("import \""+string+"\";");
		}
		writeEmptyLine();
	}
	
	private void buildFilePackage(){
		writeLine("package "+_proto.getPackage()+";");
		writeEmptyLine();
	}
	
	private void buildOptions() {
		FileOptions options = _proto.getOptions();
		String javaPackage = options.getJavaPackage();
		
		if(javaPackage!=null && !javaPackage.equals("")){
			writeLine("option java_package=\""+javaPackage+"\";");
		}
		
		String javaOuterClassname = options.getJavaOuterClassname();
		if(javaOuterClassname!=null && !javaOuterClassname.equals("")){
			writeLine("option java_outer_classname=\""+javaOuterClassname+"\";");
		}
		
		OptimizeMode optimizeMode = options.getOptimizeFor();
		//the default value is SPEED,if SPEED ,then ignore
		if(optimizeMode != OptimizeMode.SPEED){
			writeLine("option optimize_for = "+optimizeMode.name()+";");
		}
		
		writeEmptyLine();
	}
	
	private void buildPublicDependency() {
		//List<String> pdeplist = _proto.getpub
	}
	
	private void buildMessageType(DescriptorProto des) {
		writeLine("message "+des.getName()+"{");
		addIndent();
		
		List<FieldDescriptorProto> fields = des.getFieldList();
		for (FieldDescriptorProto fieldDescriptorProto : fields) {
			buildField(fieldDescriptorProto);
		}
		
		List<EnumDescriptorProto> enums = des.getEnumTypeList();
		for (EnumDescriptorProto enumDescriptorProto : enums) {
			buildEnumType(enumDescriptorProto);
		}
		
		List<DescriptorProto> nestedTypes = des.getNestedTypeList();
		for (DescriptorProto descriptorProto : nestedTypes) {
			buildMessageType(descriptorProto);			
		}
		
		List<ExtensionRange> ranges = des.getExtensionRangeList();
		for (ExtensionRange extensionRange : ranges) {
			buildExtensionRange(extensionRange);
		}
	
		buildExtend(des.getName(),des.getExtensionList());
		
		//TODO:do not know how to parse
		UnknownFieldSet unknownFieldSet = des.getUnknownFields();
		if(unknownFieldSet!=null){
			Map<Integer,Field> map = unknownFieldSet.asMap();
			for(Map.Entry<Integer, Field> entry : map.entrySet()){
				Field field = entry.getValue();
				entry.getValue().getLengthDelimitedList();
			}
		}
		
		reduceIndent();
		writeLine("}");
		writeEmptyLine();
	}
	
	private void buildField(FieldDescriptorProto des) {
		String field="";
		
		field+=_labelMap.get(des.getLabel())+" ";

		Type fieldType = des.getType();
		if(fieldType != Type.TYPE_MESSAGE && fieldType!=Type.TYPE_GROUP){
			field+=_typeMap.get(des.getType())+" ";
		}else{
			field+=des.getTypeName()+" ";
		}
		
		
		field += des.getName()+" = ";
		field += des.getNumber();

		String ext="";
		if(des.hasDefaultValue()){
			ext += "default = ";
			if(fieldType == Type.TYPE_STRING || fieldType == Type.TYPE_BYTES){
				ext +="\"";
				ext += des.getDefaultValue();
				ext +="\"";
			}else{
				ext +=des.getDefaultValue();
			}
			ext += ",";
		}

		FieldOptions options = des.getOptions();
		if(options.hasDeprecated()){
			ext += "deprecated = " + options.getDeprecated() +",";
		}
		if(options.hasCtype()){
			ext += "ctype = "+options.getCtype().name() +",";
		}
		if(options.hasLazy()){
			ext += "lazy = " + options.getLazy() + ","; 
		}
		if(options.hasPacked()){
			ext += "packed = " + options.getPacked() + ",";
		}
		if(!ext.equals("")){
			ext = ext.substring(0, ext.length()-1);
			field+=" [" + ext +"]";
		}
		field = field+";";
		writeLine(field);
	}
	
	private void buildEnumType(EnumDescriptorProto des) {
		/*
		 * enum ForeignEnum {
			  FOREIGN_FOO = 4;
			  FOREIGN_BAR = 5;
			  FOREIGN_BAZ = 6;
			}
		 * */
		writeLine("enum "+des.getName()+" {");
		addIndent();
		
		List<EnumValueDescriptorProto> eProtos = des.getValueList();
		for (EnumValueDescriptorProto enumValueDescriptorProto : eProtos) {
			buildEnum(enumValueDescriptorProto);
		}
		
		reduceIndent();
		writeLine("}");
		writeEmptyLine();
	}
	
	private void buildEnum(EnumValueDescriptorProto des) {
		writeLine(des.getName()+" = "+des.getNumber()+";");
	}


	private void buildExtensionRange(ExtensionRange des) {
		int end = des.getEnd();
		String endstr="";
		if(end == EXTENSIONS_MAX){
			endstr = "max";
		}else{
			endstr = end+"";
		}
		writeLine("extensions "+des.getStart()+" to "+ endstr +";");
	}

	//build extend where inside message
	private void buildExtend(String extendName, List<FieldDescriptorProto> protos) {
		if(protos.isEmpty()){
			return;
		}
		writeLine("extend "+extendName+" {");
		addIndent();
		for (FieldDescriptorProto fieldDescriptorProto : protos) {
			buildField(fieldDescriptorProto);
		}
		reduceIndent();
		writeLine("}");
	}
	
	//build extend where outside message
	private void buildExtend(List<FieldDescriptorProto> protos) {
		Map<String, List<FieldDescriptorProto>> map = new HashMap<String, List<FieldDescriptorProto>>();
		for (FieldDescriptorProto fieldDescriptorProto : protos) {
			String key = fieldDescriptorProto.getExtendee();
			if(map.containsKey(key)){
				map.get(key).add(fieldDescriptorProto);
			}else{
				List<FieldDescriptorProto> array=new ArrayList<FieldDescriptorProto>();
				array.add(fieldDescriptorProto);
				map.put(key, array);
			}
		}
		for(Map.Entry<String, List<FieldDescriptorProto>> entry : map.entrySet()){
			buildExtend(entry.getKey(), entry.getValue());
		}
	}
}
