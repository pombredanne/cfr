package org.benf.cfr.reader.entities;

import org.benf.cfr.reader.util.ConfusedCFRException;
import org.benf.cfr.reader.util.ListFactory;
import org.benf.cfr.reader.util.MapFactory;
import org.benf.cfr.reader.util.bytestream.ByteData;
import org.benf.cfr.reader.util.bytestream.OffsettingByteData;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: lee
 * Date: 15/04/2011
 * Time: 19:20
 * To change this template use File | Settings | File Templates.
 */
public class ConstantPool {
    private static final Logger logger = LoggerFactory.create(ConstantPool.class);

    private final long length;
    private final List<ConstantPoolEntry> entries;
    private final Map<String, String> longNameToShortName = MapFactory.newMap();
    private final Map<String, String> shortNameToLongName = MapFactory.newMap();

    public ConstantPool(ByteData raw, short count) {
        ArrayList<ConstantPoolEntry> res = new ArrayList<ConstantPoolEntry>();
        count--;
        res.ensureCapacity(count);

        length = processRaw(raw, count, res);
        entries = res;
    }

    public void dumpImports(Dumper d) {
        if (shortNameToLongName.isEmpty()) return;
        d.print("\n");
        List<String> names = ListFactory.newList(shortNameToLongName.values());
        Collections.sort(names);
        for (String shortenedName : names) {
            d.print("import " + shortenedName + ";\n");
        }
        d.print("\n");
    }

    private static long processRaw(ByteData raw, short count, List<ConstantPoolEntry> tgt) {
        OffsettingByteData data = raw.getOffsettingOffsetData(0);
        logger.info("Processing " + count + " constpool entries.");
        for (short x = 0; x < count; ++x) {
            ConstantPoolEntry.Type type = ConstantPoolEntry.Type.get(data.getS1At(0));
            ConstantPoolEntry cpe;
            switch (type) {
                case CPT_NameAndType:
                    cpe = new ConstantPoolEntryNameAndType(data);
                    break;
                case CPT_String:
                    cpe = new ConstantPoolEntryString(data);
                    break;
                case CPT_FieldRef:
                    cpe = new ConstantPoolEntryFieldRef(data);
                    break;
                case CPT_MethodRef:
                    cpe = new ConstantPoolEntryMethodRef(data, false);
                    break;
                case CPT_InterfaceMethodRef:
                    cpe = new ConstantPoolEntryMethodRef(data, true);
                    break;
                case CPT_Class:
                    cpe = new ConstantPoolEntryClass(data);
                    break;
                case CPT_Double:
                    cpe = new ConstantPoolEntryDouble(data);
                    break;
                case CPT_Float:
                    cpe = new ConstantPoolEntryFloat(data);
                    break;
                case CPT_Long:
                    cpe = new ConstantPoolEntryLong(data);
                    break;
                case CPT_Integer:
                    cpe = new ConstantPoolEntryInteger(data);
                    break;
                case CPT_UTF8:
                    cpe = new ConstantPoolEntryUTF8(data);
                    break;
                default:
                    throw new ConfusedCFRException("Invalid constant pool entry : ");
            }
            logger.info("" + (x + 1) + " : " + cpe);
            tgt.add(cpe);
            switch (type) {
                case CPT_Double:
                case CPT_Long:
                    tgt.add(null);
                    x++;
                    break;
            }

            long size = cpe.getRawByteLength();
            data.advance(size);
        }
        return data.getOffset();
    }

    public long getRawByteLength() {
        return length;
    }

    public ConstantPoolEntry getEntry(int index) {
        if (index == 0) throw new ConfusedCFRException("Attempt to fetch element 0 from constant pool");
        // NB: Constant pool entries are 1 based.
        return entries.get(index - 1);
    }
//
//    private void setEntry(int index, ConstantPoolEntry constantPoolEntry) {
//        if (index == 0) throw new ConfusedCFRException("Attempt to set element 0 in constant pool");
//        // NB: Constant pool entries are 1 based.
//        entries.set(index - 1, constantPoolEntry);
//    }

    public ConstantPoolEntryUTF8 getUTF8Entry(int index) {
        return (ConstantPoolEntryUTF8) getEntry(index);
    }

    public ConstantPoolEntryNameAndType getNameAndTypeEntry(int index) {
        return (ConstantPoolEntryNameAndType) getEntry(index);
    }

    public ConstantPoolEntryClass getClassEntry(int index) {
        return (ConstantPoolEntryClass) getEntry(index);
    }

    public void markClassNameUsed(String className) {
        int idxlast = className.lastIndexOf('/');
        String partname = idxlast == -1 ? className : className.substring(idxlast + 1);
        if (!shortNameToLongName.containsKey(partname)) {
            shortNameToLongName.put(partname, className.replace('/', '.'));
            longNameToShortName.put(className, partname);
        }
    }

    public String getDisplayableClassName(String className) {
        String res = longNameToShortName.get(className);
        if (res == null) return res;
        return res.replace('/', '.');
    }
}
