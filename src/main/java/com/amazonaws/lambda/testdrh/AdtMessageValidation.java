package com.amazonaws.lambda.testdrh;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v251.datatype.CX;
import ca.uhn.hl7v2.model.v251.datatype.TS;
import ca.uhn.hl7v2.model.v251.datatype.XPN;
import ca.uhn.hl7v2.model.v251.message.ADT_AXX;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.validation.impl.NoValidation;

public class AdtMessageValidation {
	
private static String FHIRDateFormat = "yyyy-MM-dd'T'HH:mm:ss";

private final static String delimiter = "|";
	
public static String AdtMsgValidation(ADT_AXX adtHapiObj)
{
	String trigger = adtHapiObj.getMSH().getMsh9_MessageType().getMsg2_TriggerEvent().getValue();
	String errorString = "";	
		 
	errorString = ADT_Common_Validation(adtHapiObj,errorString);
	
	
	
	switch (trigger.toUpperCase()) {
	  case "A01":
		  errorString = ADT_A01_Validation(adtHapiObj,errorString); 
	      break;
	  case "A03":
	  case "A04":
	  case "A06":	  
	  case "A07":	  
		  errorString = ADT_A03_A04_A06_A07_CommonValidation(adtHapiObj,errorString,trigger);
	      break;
	}

 return errorString;
}
	

//COMMON VALIDATIONS
private static String ADT_Common_Validation(ADT_AXX Hapi, String errorString)
{
	int familyNameValidationErrors = 0;
	int givenNameValidationErrors = 0;
	int patientIdErrors = 0;
	int identifierTypeCodeErrors = 0;
	int assigningAuthorityErrors = 0;


	
	
	String namespaceID = Hapi.getMSH().getSendingFacility().getNamespaceID().getValue();
	String messageCode =  Hapi.getMSH().getMsh9_MessageType().getMsg1_MessageCode().getValue();
	String admitDate = Hapi.getPV1().getPv144_AdmitDateTime().getTime().getValue();
	String patAcctNumber = Hapi.getPID().getPid18_PatientAccountNumber().getIDNumber().getValue();
	XPN[] patName = Hapi.getPID().getPid5_PatientName(); // check for both family and given name
	CX[] patIdent = Hapi.getPID().getPid3_PatientIdentifierList();  	
	String patDob = Hapi.getPID().getPid7_DateTimeOfBirth().getTime().getValue();
	String patGender = Hapi.getPID().getPid8_AdministrativeSex().getValue();
	
	//Start of retreiving provenance
	
	String SendingApp = Hapi.getMSH().getMsh3_SendingApplication().getNamespaceID().getValue();
	String SendingFac = Hapi.getMSH().getMsh4_SendingFacility().getNamespaceID().getValue();
	String DateTimeMsg = Hapi.getMSH().getMsh7_DateTimeOfMessage().getTime().getValue();
	String MsgType =  Hapi.getMSH().getMsh9_MessageType().getMsg1_MessageCode().getValue();
	String MsgCode = Hapi.getMSH().getMsh9_MessageType().getMsg2_TriggerEvent().getValue();
	String Hl7Ver = Hapi.getMSH().getMsh12_VersionID().getVid1_VersionID().getValue();
	
	//following allows iteration on the patient name and patient identifier arrays
	DateUtil Dateutil = null;
	//namespaceid cannot be null or blank or spaces
	String DateFormat_dob = null;	
	String DateFormat_admit = null;
	
	if(patDob != null && !patDob.isEmpty())
	{
	DateFormat_dob = DateUtil.determineDateFormat(patDob);
	}	 
	 
	if(admitDate != null && !admitDate.isEmpty() )
	{	
	
	DateFormat_admit = DateUtil.determineDateFormat(admitDate);
	}
	
	if (IsBlankOrAllSpaces(SendingApp))
	{
		errorString +=  "Sending Application contains null or blank spaces. " + delimiter;
	}
	
	if (IsBlankOrAllSpaces(SendingFac))
	{
		errorString +=  "Sending Facility contains null or blank spaces. " + delimiter;
	}
	
	
	if (IsBlankOrAllSpaces(DateTimeMsg))
	{
		errorString +=  "DateTime stamp contains null or blank spaces. " + delimiter;
	}
	
	if (IsBlankOrAllSpaces(MsgType))
	{
		errorString +=  "DateTime stamp contains null or blank spaces. " + delimiter;
	}
	
	
	if (IsBlankOrAllSpaces(patGender))
	{
		errorString +=  "Gender contains null or blank spaces. " + delimiter;
	}
	
	if (IsBlankOrAllSpaces(patDob))
	{
		errorString += " Date of birth contains null or blank spaces. " + delimiter;
	}
	
	//else if(IsInvalidFormatDate(patDob,SimpleDateFormat))
	//FHIRDateFormat
	
	if (DateFormat_dob==null || IsInvalidFormatDate(patDob,FHIRDateFormat,DateFormat_dob))
	{
		errorString += " Date of birth has invalid date format. " + delimiter;
	}
	
	
	
	
	else if (IsFutureDate(patDob,DateFormat_dob))
	{
		errorString +=  " Date of birth has future date. " + delimiter;
	}
	
	
	if (IsBlankOrAllSpaces(namespaceID))
	{
		errorString +=  " Facility Provider contains null or blank spaces. " + delimiter;
	}
	
	if (IsFieldNotSetToCorrectString(((messageCode == null) ? "" : messageCode),"ADT"))
	{
		errorString +=  " MessageCode not set to ADT. " + delimiter ;
	}
	
	if (IsBlankOrAllSpaces(admitDate))
	{
		errorString +=  " Admit date contains null or blank spaces. " + delimiter;
	}
	
		
	
	//else if(IsInvalidFormatDate(admitDate,SimpleDateTimeFormat))
	if(DateFormat_admit==null ||  IsInvalidFormatDate(admitDate,FHIRDateFormat,DateFormat_admit)){
		errorString +=  " Admit date has invalid date format. " + delimiter;
	}
	else if (IsFutureDate(admitDate,DateFormat_admit))
	{
		errorString +=  " Admit date has future date. " + delimiter;
	}
	
	if (IsBlankOrAllSpaces(patAcctNumber))
	{
		errorString +=  " Patient account number contains null or blank spaces. " + delimiter;
	}
	
	//Now iterate through array and see if either family name or given name and keep count of number of identical issues
	
	
	for( int i= 0;i<patName.length; i++)
	{
		if (IsBlankOrAllSpaces(patName[i].getXpn1_FamilyName().getFn1_Surname().getValue()))
		{
			familyNameValidationErrors++;
		}
		
		
		if (IsBlankOrAllSpaces(patName[i].getXpn2_GivenName().getValue()))
		{
			givenNameValidationErrors++;
		}
	
			
	};
	
	
	for( int i= 0;i<patIdent.length; i++)
	{
		
		if (IsBlankOrAllSpaces(patIdent[i].getCx1_IDNumber().getValue()))
			
		{
			patientIdErrors++;
		}
		
		if (IsBlankOrAllSpaces(patIdent[i].getCx5_IdentifierTypeCode().getValue()))
		{
			identifierTypeCodeErrors++;
		}
				
		if (IsBlankOrAllSpaces(patIdent[i].getCx4_AssigningAuthority().getHd1_NamespaceID().getValue()))
		{
			assigningAuthorityErrors++;
		}
		
		
		//Diagnosis details validations
		
		
		
	};
	
	//familyValidationErrors
	//after validation if error exists on any of the validated fields post to Kafka error topic
	
	if(familyNameValidationErrors > 0)
	{
		errorString +=  " There were " + Integer.toString(familyNameValidationErrors) + " family Name validation errors validation errors." + delimiter;
		
	}
	
	if(givenNameValidationErrors > 0)
	{
		errorString +=  " There were " + Integer.toString(givenNameValidationErrors) + " given name validation errors." + delimiter;
		
	}
	
	if(patientIdErrors > 0)
	{
		errorString +=  " There were " + Integer.toString(patientIdErrors) + " Patientid validation errors." + delimiter;
		
	}
	
	if(identifierTypeCodeErrors > 0)
	{
		errorString +=  " There were " + Integer.toString(identifierTypeCodeErrors) + " Identifier type validation errors." + delimiter;
		
	}
	
	if(assigningAuthorityErrors > 0)
	{
		errorString +=  " There were " + Integer.toString(identifierTypeCodeErrors) + " Assigning authority validation errors." + delimiter;
		
	}
	
	return errorString;
}

//A01 VALIDATIONS
private static String ADT_A01_Validation(ADT_AXX Hapi, String errorString)
{
	
	String triggerEvent = Hapi.getMSH().getMsh9_MessageType().getTriggerEvent().getValue();
	String eventTypeCode = Hapi.getEVN().getEventTypeCode().getValue();
	
	
	//if (IsFieldNotSetToCorrectString(((messageCode == null) ? "" : messageCode),"ADT"))
	
	if(IsFieldNotSetToCorrectString(((triggerEvent == null) ? "": triggerEvent),"A01"))
	{
		errorString +=  " Trigger event contain invalid data. " + delimiter;
	}
	
	
	if (IsFieldNotSetToCorrectString(((eventTypeCode == null) ? "" : eventTypeCode),"A01"))
	//if(IsFieldNotSetToCorrectString(eventTypeCode,"A01"))
	{
		errorString +=  " Event type code contains invalid data. " + delimiter;
	}
	
	//diagnosis code validation

	if(Hapi.getDG1().getDiagnosisCodeDG1().getIdentifier().getValue() !=null){
		String code = CheckICD(Hapi.getDG1().getDiagnosisCodeDG1().getIdentifier().getValue());
		if(code==null){
			errorString += "Invalid Diagnosis code"+ delimiter;
		}
	}


	
	return errorString;
}

// A03 A04 VALIDATIONS
private static String ADT_A03_A04_A06_A07_CommonValidation(ADT_AXX Hapi, String errorString,String trigger)
{
	int dischargeDateSpaceErrors=0;
	int dischargeDateFormatErrors=0;
	int dischargeDateFutureDateErrors=0;
	
	String triggerEvent = Hapi.getMSH().getMsh9_MessageType().getTriggerEvent().getValue();
	String eventTypeCode = Hapi.getEVN().getEventTypeCode().getValue();
	TS[] dischargeDate = Hapi.getPV1().getPv145_DischargeDateTime();
	String dischargeDateString;
	

if (triggerEvent.equals(trigger))
	{
		
	if(IsFieldNotSetToCorrectString(triggerEvent,trigger))
	{
		errorString +=  " Trigger event contain invalid data. " + delimiter;
	}
	
	if(IsFieldNotSetToCorrectString(((eventTypeCode == null) ? "": eventTypeCode),trigger))
	//if(IsFieldNotSetToCorrectString(eventTypeCode,"A03"))
	{
		errorString +=  " Event type code contains invalid data. " + delimiter;
	}

}





if (triggerEvent.equals("A03"))
{

	for( int i= 0;i<dischargeDate.length; i++)
	{
		
		dischargeDateString = dischargeDate[i].getTs1_Time().getValue();
		
		String DateFormat_discharge = DateUtil.determineDateFormat(dischargeDateString);
		
		
		
		if (IsBlankOrAllSpaces(dischargeDateString))
		{
			dischargeDateSpaceErrors++;
		}
		
		
		//if(DateFormat_admit==null ||  IsInvalidFormatDate(patDob,FHIRDateFormat,admitDate))
		
		if(IsInvalidFormatDate(dischargeDateString,FHIRDateFormat,DateFormat_discharge))
			
		{
			
			dischargeDateFormatErrors++;
		}
		
		else if (IsFutureDate(dischargeDateString,DateFormat_discharge))
		{
			dischargeDateFutureDateErrors++;
		}
	
	
	}
	
}
	if(dischargeDateSpaceErrors>0)
	{
		errorString +=  " There is/are " + Integer.toString(dischargeDateSpaceErrors) + " Discharge date(s) that have spaces. " + delimiter;
	}
	
	if(dischargeDateFormatErrors>0)
	{
		errorString +=  " There is/are " + Integer.toString(dischargeDateFormatErrors) + " Discharge date(s) that have invalid format(s). " + delimiter;
	}
	
	
	if(dischargeDateFutureDateErrors>0)
	{
		errorString +=  " There is/are " + Integer.toString(dischargeDateFutureDateErrors) + " Discharge date(s) that are in the future. " + delimiter;
	}
	
	
	//diagnosis code validation
	if(Hapi.getDG1().getDiagnosisCodeDG1().getIdentifier().getValue() !=null){
		String code = CheckICD(Hapi.getDG1().getDiagnosisCodeDG1().getIdentifier().getValue());
		if(code==null){
			errorString += "Invalid Diagnosis code"+ delimiter;
		}
	}
	
	
	return errorString;
}



/**
 * LOCAL HELPER METHODS
 *
 */
private static boolean IsBlankOrAllSpaces(String s)
{
    return (s == null) || (s.trim().length() == 0);
}


private static boolean IsFutureDate(String dateString, String dateFormat) 
{
	SimpleDateFormat dateFormatter = new SimpleDateFormat(dateFormat);
	
	try
	{
	
		
	Date current = new Date();	
		
	Date date = dateFormatter.parse(dateString);
    //Long l = date.getTime();
    //Date next = new Date(l);
	
    if(date.after(current))
    {
    	return true;
    }
	}
	catch (ParseException e) {
        e.printStackTrace();
        return true;
    }
	
	
	return false;
}


private static boolean IsInvalidFormatDate(String dateString,String WanteddateFormat,String OlddateFormat)
{
	//check for valid date, if so check that not future date
	
	
	String result;
	
	
	
	try {
		SimpleDateFormat dateFormatterWanted = new SimpleDateFormat(OlddateFormat);
		SimpleDateFormat dateFormatterOld = new SimpleDateFormat(OlddateFormat);
		dateFormatterWanted.setLenient(false);
		dateFormatterOld.setLenient(false);
		
		result = dateFormatterWanted.format(dateFormatterOld.parse(dateString));
		
	    } 
	
	
	
	catch (ParseException e) {
        
        return true;
    }
	
	
	catch(Exception e) {
        return true;
        
    }
	
	
	return false;
	
}


private static boolean IsFieldNotSetToCorrectString(String field, String checkString)
{
	
return (!field.equals(checkString));
	
	
}

public static ADT_AXX parseXmlToHapi(String HL7_XML) throws HL7Exception
{
	DefaultHapiContext defHapiCtx = new DefaultHapiContext();
	defHapiCtx.setValidationContext(new NoValidation());
	defHapiCtx.setModelClassFactory(new CanonicalModelClassFactory(ADT_AXX.class));
	Message message = defHapiCtx.getXMLParser().parse(HL7_XML);
	ADT_AXX axx = null;

	axx = (ADT_AXX) message;
	return axx;
}

public static String CheckICD(String input){
    //saving a copy of input in separate variable
    String result = input;
    //remove the dot from input
    input = input.replace(".", "");

   boolean valid = true;
    //This if statement is to check if the input is ICD10
    if(input.length()>=3 && input.length()<=7){ //check if its length is between 3 and 7
        if(Character.isAlphabetic(input.charAt(0))){    //check if its first character is alphabet
            //check if its second and third characters are numberic
            if(Character.isDigit(input.charAt(1)) && Character.isDigit(input.charAt(2))){
                //loop to check all ther characters
                for(int a=3;a<input.length();a++){
                    //check if all other characters are numerics or alphabets
                    if(Character.isAlphabetic(input.charAt(a)) || Character.isDigit(input.charAt(a))){

                   }
                    else valid = false; //if a invalid character is found
                }
                //if its all fine,
                if(valid == true){
                    //generate the result and return it
                    result = "ICD10|"+result;
                    return result;
                }
            }
        }
    }
    //This if statement is to check if the input is ICD9
    else if(input.length()>=3 && input.length()<=5){ //check if its length is between 3 and 5
        //check if its first character is alphabet or numberic
        char e = 'E';
        char v = 'V';
        if( ( (Character.isAlphabetic(input.charAt(0)))&&( (input.charAt(0)==e)  || (input.charAt(0)==v) ) ) || Character.isDigit(input.charAt(0))){
            //loop to check all other characters
            for(int a=1;a<input.length();a++){
                //check if all other characters are numerics
                if(Character.isDigit(input.charAt(a))){
                }
                else valid = false; //if a single alhabet is found
            }
            //if its all fine,
            if(valid == true){
                //generate the result and return it
                result = "ICD9|"+result;
                return result;
            }
        }
    }
   return null; //error message is returned, if the input is neither ICD9 nor ICD10.
    
}
}

	