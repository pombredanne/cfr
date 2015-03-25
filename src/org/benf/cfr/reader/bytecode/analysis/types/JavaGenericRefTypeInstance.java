package org.benf.cfr.reader.bytecode.analysis.types;

import org.benf.cfr.reader.bytecode.analysis.parse.utils.ComparableUnderEC;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.DefaultEquivalenceConstraint;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.EquivalenceConstraint;
import org.benf.cfr.reader.entities.constantpool.ConstantPool;
import org.benf.cfr.reader.state.TypeUsageCollector;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.ListFactory;
import org.benf.cfr.reader.util.MiscConstants;
import org.benf.cfr.reader.util.output.CommaHelp;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.ToStringDumper;

import java.util.List;

public class JavaGenericRefTypeInstance implements JavaGenericBaseInstance, ComparableUnderEC {
    private static final WildcardConstraint WILDCARD_CONSTRAINT = new WildcardConstraint();

    private final JavaRefTypeInstance typeInstance;
    private final List<JavaTypeInstance> genericTypes;
    private final boolean hasUnbound;

    public JavaGenericRefTypeInstance(JavaTypeInstance typeInstance, List<JavaTypeInstance> genericTypes) {
        if (!(typeInstance instanceof JavaRefTypeInstance)) {
            throw new IllegalStateException("Generic sitting on top of non reftype");
        }
        this.typeInstance = (JavaRefTypeInstance) typeInstance;
        this.genericTypes = genericTypes;
        boolean unbound = false;
        for (JavaTypeInstance type : genericTypes) {
            if (type instanceof JavaGenericBaseInstance) {
                if (((JavaGenericBaseInstance) type).hasUnbound()) {
                    unbound = true;
                    break;
                }
            }
        }
        hasUnbound = unbound;
    }

    @Override
    public void collectInto(TypeUsageCollector typeUsageCollector) {
        typeUsageCollector.collectRefType(typeInstance);
        for (JavaTypeInstance genericType : genericTypes) {
            typeUsageCollector.collect(genericType);
        }
    }

    @Override
    public boolean hasUnbound() {
        return hasUnbound;
    }

    @Override
    public boolean hasForeignUnbound(ConstantPool cp) {
        if (!hasUnbound) return false;
        for (JavaTypeInstance type : genericTypes) {
            if (type instanceof JavaGenericBaseInstance) {
                if (((JavaGenericBaseInstance) type).hasForeignUnbound(cp)) return true;
            }
        }
        return false;
    }

    @Override
    public boolean isObject() {
        return true;
    }

    @Override
    public JavaGenericRefTypeInstance getBoundInstance(GenericTypeBinder genericTypeBinder) {
        if (genericTypeBinder == null) {
            return this;
        }
        List<JavaTypeInstance> res = ListFactory.newList();
        for (JavaTypeInstance genericType : genericTypes) {
            res.add(genericTypeBinder.getBindingFor(genericType));
        }
        return new JavaGenericRefTypeInstance(typeInstance, res);
    }

    @Override
    public boolean tryFindBinding(JavaTypeInstance other, GenericTypeBinder target) {
        boolean res = false;
        if (other instanceof JavaGenericRefTypeInstance) {
            // We can dig deeper.
            JavaGenericRefTypeInstance otherJavaGenericRef = (JavaGenericRefTypeInstance) other;
            if (genericTypes.size() == otherJavaGenericRef.genericTypes.size()) {
                for (int x = 0; x < genericTypes.size(); ++x) {
                    JavaTypeInstance genericType = genericTypes.get(x);
                    if (genericType instanceof JavaGenericBaseInstance) {
                        JavaGenericBaseInstance genericBaseInstance = (JavaGenericBaseInstance) genericType;
                        res |= genericBaseInstance.tryFindBinding(otherJavaGenericRef.genericTypes.get(x), target);
                    }
                }
            }
        }
        return res;
    }

    @Override
    public StackType getStackType() {
        return StackType.REF;
    }

    @Override
    public void dumpInto(Dumper d, TypeUsageInformation typeUsageInformation) {
        d.dump(typeInstance).print('<');
        boolean first = true;
        for (JavaTypeInstance type : genericTypes) {
            first = CommaHelp.comma(first, d);
            d.dump(type);
        }
        d.print('>');
    }

    @Override
    public String toString() {
        return new ToStringDumper().dump(this).toString();
    }

    @Override
    public JavaTypeInstance getArrayStrippedType() {
        return this;
    }

    @Override
    public List<JavaTypeInstance> getGenericTypes() {
        return genericTypes;
    }

    @Override
    public JavaRefTypeInstance getDeGenerifiedType() {
        return typeInstance;
    }

    @Override
    public int getNumArrayDimensions() {
        return 0;
    }

    @Override
    public int hashCode() {
        int hash = 31 + typeInstance.hashCode();
        return hash;
    }

    @Override
    public String getRawName() {
        return new ToStringDumper().dump(this).toString();
    }

    @Override
    public InnerClassInfo getInnerClassHereInfo() {
        return typeInstance.getInnerClassHereInfo();
    }

    public JavaTypeInstance getTypeInstance() {
        return typeInstance;
    }

    @Override
    public BindingSuperContainer getBindingSupers() {
        return typeInstance.getBindingSupers();
    }


    @Override
    public boolean equals(Object o) {
        return equivalentUnder(o, DefaultEquivalenceConstraint.INSTANCE);
    }

    @Override
    public boolean equivalentUnder(Object o, EquivalenceConstraint constraint) {
        if (o == this) return true;
        if (!(o instanceof JavaGenericRefTypeInstance)) return false;
        JavaGenericRefTypeInstance other = (JavaGenericRefTypeInstance) o;
        if (!constraint.equivalent(typeInstance, other.typeInstance)) return false;
        if (!constraint.equivalent(genericTypes, other.genericTypes)) return false;
        return true;
    }


    @Override
    public boolean isComplexType() {
        return true;
    }

    @Override
    public boolean isUsableType() {
        return true;
    }

    @Override
    public JavaTypeInstance removeAnArrayIndirection() {
        return this;
    }

    @Override
    public RawJavaType getRawTypeOfSimpleType() {
        return RawJavaType.REF;
    }

    @Override
    public boolean implicitlyCastsTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        if (other == TypeConstants.OBJECT) return true;
        if (this.equivalentUnder(other, WILDCARD_CONSTRAINT)) return true;
        BindingSuperContainer bindingSuperContainer = getBindingSupers();
        if (bindingSuperContainer == null) return false;
        JavaTypeInstance degenerifiedOther = other.getDeGenerifiedType();
        JavaTypeInstance degenerifiedThis = getDeGenerifiedType();
        if (degenerifiedThis.equals(other)) return true;

        if (!bindingSuperContainer.containsBase(degenerifiedOther)) return false;
        // If this was cast to the type of other, what would it be?
        JavaTypeInstance boundBase = bindingSuperContainer.getBoundSuperForBase(degenerifiedOther);
        if (other.equals(boundBase)) return true;
        if (degenerifiedOther.equals(other)) return true;

        if (gtb != null) {
            JavaTypeInstance reboundBase = (gtb.getBindingFor(boundBase));
            if (other.equals(reboundBase)) return true;


            JavaTypeInstance reboundOther = (gtb.getBindingFor(other));
            if (this.equivalentUnder(reboundOther, WILDCARD_CONSTRAINT)) return true;
        }
        return false;
    }

    @Override
    public boolean canCastTo(JavaTypeInstance other, GenericTypeBinder gtb) {
        return true;
    }

    @Override
    public String suggestVarName() {
        return typeInstance.suggestVarName();
    }

    /*
     * Note we test o_2_!
     */
    public static class WildcardConstraint extends DefaultEquivalenceConstraint {
        @Override
        public boolean equivalent(Object o1, Object o2) {
            if (o2 instanceof JavaGenericPlaceholderTypeInstance) {
                if (((JavaGenericPlaceholderTypeInstance) o2).getRawName().equals(MiscConstants.UNBOUND_GENERIC))
                    return true;
            }
            return super.equivalent(o1, o2);
        }
    }
}
