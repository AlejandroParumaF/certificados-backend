package com.certificados.service;

import com.jcraft.jsch.*;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipOutputStream;
import org.apache.pdfbox.Loader;

@Service
public class ApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationService.class);

    @Autowired
    private JavaMailSender emailSender;

    @Value("${sftp.host}")
    private String host;

    @Value("${sftp.port}")
    private int port;

    @Value("${sftp.username}")
    private String username;

    @Value("${sftp.password}")
    private String password;

    @Value("${sftp.remoteDirectory}")
    private String remoteDirectory;

    public int getDocumentCount(String anio, String nit) {
        int documentCount = 0;
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // Desactivar la verificación estricta del host (¡solo para pruebas!)
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;

            channelSftp.cd(remoteDirectory + "/" + anio); // Cambia al directorio remoto

            Vector<ChannelSftp.LsEntry> files = channelSftp.ls("*"); // Lista todos los archivos y directorios

            for (ChannelSftp.LsEntry entry : files) {
                if (!entry.getAttrs().isDir()) { // Excluye directorios

                    String[] filename = entry.getFilename().split("-");

                    if (filename.length > 2) { // Asegúrate de que haya al menos 3 partes
                        String nitArchivo = filename[1];
                        String anioArchivo = filename[2];

                        if (nitArchivo.equals(nit) && anioArchivo.contains(anio)) { //Usamos contains en vez de equals para el anio
                            documentCount++;
                        }
                    }
                }
            }

            logger.info("Numero de documentos en {}: {}", remoteDirectory, documentCount);
            return documentCount;

        } catch (JSchException | SftpException e) {
            logger.error("Error al conectar o acceder al SFTP: ", e);
            return -1; // -1 indica un error
        } finally {
            if (channelSftp != null) {
                try {
                    channelSftp.exit();
                } catch (Exception e) {
                    logger.error("Error al cerrar el canal SFTP: ", e);
                }
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    public int enviarCorreo(String anio, String nit, String nombre, String email) {
        try {
            byte[] zipBytes = createZip(anio, nit, nombre, email); // Crear el archivo ZIP

            if (zipBytes == null) {
                System.err.println("Error al crear el archivo ZIP desde el SFTP.");
                return -2; // Error al crear el archivo ZIP
            }

            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name()); // Habilitar multipart y UTF-8

            helper.setFrom("oetm4682@coomeva.com.co");
            helper.setTo(email);
            helper.setSubject("Certificados Tributarios Periodo " + anio);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            String fechaActual = dateFormat.format(new Date());
            String zipFileName = "certificados_" + fechaActual + ".zip";

            helper.addAttachment(zipFileName, new ByteArrayResource(zipBytes));

            // Construir el HTML del cuerpo del correo
            String content = "<html><body>" +
                    "<p>Estimado proveedor,</p>" +
                    "<p>Conforme lo establecido en el artículo 1.6.1.13.2.40 del Decreto Único Reglamentario y el artículo 381 del estatuto tributario, anexamos los certificados tributarios expedidos correspondiente al periodo fiscal " + anio + ".</p>" +
                    "<p>Si tiene problemas con la descarga de su certificado, enviar un correo electrónico a contabilidad_fiducoomeva@coomeva.com.co informando el inconveniente presentado y relacionando el NIT del proveedor solicitante.</p>" +
                    "<p>Cordial Saludo,</p>" +
                    "<img src='cid:firma' width='900' height='230'>" + // Ajusta width y height según necesites
                    "</body></html>";

            helper.setText(content, true); // El segundo parámetro 'true' indica que es HTML

            // Adjuntar la imagen como un recurso incrustado (inline)
            ClassPathResource resource = new ClassPathResource("static/firma.png"); // Ajusta la ruta a tu imagen
            helper.addInline("firma", resource);
            emailSender.send(message); // Enviar el correo

            System.out.println("Correo enviado exitosamente a " + email); // Agrega un mensaje de éxito
            return 1; // Éxito

        } catch (MessagingException e) {
            System.err.println("Error al enviar el correo: " + e.getMessage());
            return -2; // Error al enviar el correo
        } catch (Exception e) {
            System.err.println("Error inesperado: " + e.getMessage());
            return -2; // Error inesperado
        }
    }

    public String getCurrentDate() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }

    private byte[] encryptPdf(byte[] pdfBytes, String password) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            AccessPermission ap = new AccessPermission();
            // Establecer permisos (opcional)
            ap.setCanPrint(true);
            ap.setCanModify(false);

            StandardProtectionPolicy spp = new StandardProtectionPolicy(password, password, ap);
            spp.setEncryptionKeyLength(256); // Puedes usar 128 o 256

            document.protect(spp);
            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            logger.error("Error al encriptar el archivo PDF: ", e);
            throw new IOException("Error al encriptar el archivo PDF", e);
        }
    }

    public byte[] createZip(String anio, String nit, String nombre, String email) {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);

            // Desactivar la verificación estricta del host (¡solo para pruebas!)
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect();

            Channel channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;

            String remotelogsDirectory = remoteDirectory + "/logs";
            channelSftp.cd(remotelogsDirectory);

            String message = "Nombre: " + nombre + "\n" + "Email: " + email + "\n" + "NIT:" + nit + "\n" + "Año:" + anio + "\n" + "Fecha: " + getCurrentDate();

            // Convertir el mensaje a un InputStream
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            InputStream messageStream = new ByteArrayInputStream(messageBytes);

            // Crear y escribir en el archivo .txt en el SFTP
            channelSftp.put(messageStream, getCurrentDate() + ".txt");

            String remoteAnioDirectory = remoteDirectory + "/" + anio;
            channelSftp.cd(remoteAnioDirectory); // Cambia al directorio remoto del año

            Vector<ChannelSftp.LsEntry> files = channelSftp.ls("*"); // Lista todos los archivos y directorios

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            for (ChannelSftp.LsEntry entry : files) {
                if (!entry.getAttrs().isDir()) { // Excluye directorios
                    String filename = entry.getFilename();
                    String[] filenameParts = filename.split("-");

                    if (filenameParts.length > 2) { // Asegúrate de que haya al menos 3 partes
                        String nitArchivo = filenameParts[1];
                        String anioArchivo = filenameParts[2];

                        if (nitArchivo.equals(nit) && anioArchivo.contains(anio)) { //Usamos contains en vez de equals para el anio
                            // Descarga el archivo y lo añade al ZIP
                            try (InputStream is = channelSftp.get(filename);
                                 ByteArrayOutputStream fileBaos = new ByteArrayOutputStream()) {

                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = is.read(buffer)) > 0) {
                                    fileBaos.write(buffer, 0, len);
                                }
                                byte[] pdfBytes = fileBaos.toByteArray();

                                // Encripta el PDF con el NIT como contraseña
                                byte[] encryptedPdfBytes = encryptPdf(pdfBytes, nit);

                                // Crea la entrada ZIP con el archivo encriptado
                                String[] file = entry.getFilename().split("-");
                                String newFilename = "certificado_" + file[0] + "_" + file[2];
                                ZipEntry zipEntry = new ZipEntry(newFilename);
                                zos.putNextEntry(zipEntry);
                                zos.write(encryptedPdfBytes);
                                zos.closeEntry();

                                logger.info("Archivo {} encriptado y añadido al zip.", filename);

                            } catch (SftpException | IOException e) {
                                logger.error("Error al procesar el archivo {} para el ZIP: ", filename, e);
                            }
                        }
                    }
                }
            }

            zos.close(); // Cierra el ZipOutputStream antes de devolver los bytes

            return baos.toByteArray(); // Devuelve los bytes del ZIP

        } catch (JSchException | SftpException | IOException e) {
            logger.error("Error al conectar o acceder al SFTP: ", e);
            return null; // Devuelve null si hay un error
        } finally {
            if (channelSftp != null) {
                try {
                    channelSftp.exit();
                } catch (Exception e) {
                    logger.error("Error al cerrar el canal SFTP: ", e);
                }
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
