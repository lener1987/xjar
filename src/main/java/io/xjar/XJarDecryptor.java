package io.xjar;

import io.xjar.key.XKey;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;

import java.io.*;
import java.util.zip.Deflater;

/**
 * ZIP压缩包解密器
 *
 * @author Payne 646742615@qq.com
 * 2018/11/22 15:27
 */
public class XJarDecryptor extends XWrappedDecryptor implements XDecryptor {
    private final int level;

    public XJarDecryptor(XDecryptor xDecryptor) {
        this(xDecryptor, Deflater.DEFAULT_COMPRESSION);
    }

    public XJarDecryptor(XDecryptor xDecryptor, int level) {
        super(xDecryptor);
        this.level = level;
    }

    @Override
    public void decrypt(XKey key, File src, File dest) throws IOException {
        try (
                FileInputStream fis = new FileInputStream(src);
                FileOutputStream fos = new FileOutputStream(dest)
        ) {
            decrypt(key, fis, fos);
        }
    }

    @Override
    public void decrypt(XKey key, InputStream in, OutputStream out) throws IOException {
        JarArchiveInputStream zis = null;
        JarArchiveOutputStream zos = null;
        try {
            zis = new JarArchiveInputStream(in);
            zos = new JarArchiveOutputStream(out);
            zos.setLevel(level);
            NoCloseInputStream nis = new NoCloseInputStream(zis);
            NoCloseOutputStream nos = new NoCloseOutputStream(zos);
            ArchiveEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                zos.putArchiveEntry(new JarArchiveEntry(entry.getName()));
                if (entry.getName().endsWith(".jar")) {
                    decrypt(key, nis, nos);
                } else {
                    try (OutputStream eos = decrypt(key, nos)) {
                        XKit.transfer(zis, eos);
                    }
                }
                zos.closeArchiveEntry();
            }
            zos.finish();
        } finally {
            XKit.close(zis);
            XKit.close(zos);
        }
    }

}
