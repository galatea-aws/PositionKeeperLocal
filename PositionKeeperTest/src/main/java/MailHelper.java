import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class MailHelper {
	public static Logger logger = LogManager.getLogger(MailHelper.class.getName());
	
	public MailHelper(){
		
	}
	
	public static void sendJobCompleteMail(ArrayList<String> queryList, Properties benchmarkProp) {
		
		String lastestReportFolderPath = benchmarkProp.getProperty("gitfolder")+"/report/LastestReport/";
		final String username = "";
		final String password = "";
 
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "");
		props.put("mail.smtp.port", "");
		
		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		  });
 
		try { 
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("aws@galatea-associates.com"));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse("aws@galatea-associates.com"));
			message.setSubject("PositionKeeper Benchmark Completed");
			
			//add log
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");
			String now = sdf.format(new Date());
			Multipart multipart = new MimeMultipart();
			DataSource source = new FileDataSource("PositionKeeperlog.log");
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(now + "_PositionKeeperlog.log");
			multipart.addBodyPart(messageBodyPart);
			
			String mailInfo = "Result for following queries:\n";
			for(String queryName: queryList){
				mailInfo+=queryName+"\n";
				source = new FileDataSource(lastestReportFolderPath + queryName + ".csv");
				messageBodyPart = new MimeBodyPart();
				messageBodyPart.setDataHandler(new DataHandler(source));
				messageBodyPart.setFileName(queryName + ".csv");
				multipart.addBodyPart(messageBodyPart);
			}
			message.setText(mailInfo);
			message.setContent(multipart);
			
			Transport.send(message);
 
			System.out.println("Mail has been sent");
 
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}
	
	public static void sendJobFailMail() {
		 
		final String username = "";
		final String password = "";
 
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "");
		props.put("mail.smtp.port", "");
		
		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		  });
 
		try {
			//Set mail info
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress("aws@galatea-associates.com"));
			message.setRecipients(Message.RecipientType.TO,
				InternetAddress.parse("aws@galatea-associates.com"));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");
			String now = sdf.format(new Date());
			message.setSubject("PositionKeeper Benchmark Failed " + now);
			
			//Add log file
			Multipart multipart = new MimeMultipart();
			DataSource source = new FileDataSource("PositionKeeperlog.log");
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(now + "_PositionKeeperlog.log");
			multipart.addBodyPart(messageBodyPart);
			
			//Send mail
			Transport.send(message);
 
			System.out.println("Mail has been sent");
 
		} catch (MessagingException e) {
			e.printStackTrace();
		}
	}
}
