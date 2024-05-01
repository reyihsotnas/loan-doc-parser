package me.siyer.dev.loan;

import java.util.LinkedList;
import java.util.List;

public final class AmortisationValueHolder {
        private String derivedHeader;
        private String evaluatedHeader;
        private String value;

       private AmortisationValueHolder(){}
        private AmortisationValueHolder(final String derivedHeader, final String evaluatedHeader, final String value){
           this.derivedHeader=derivedHeader;
           this.evaluatedHeader = evaluatedHeader;
           this.value=value;
        }

        public String getEvaluatedHeader() {
            return evaluatedHeader;
        }

        public String getValue(){
            return value;
        }
        public String toString(){
           return "[ EvaluatedHeader: "+evaluatedHeader+", DerivedHeader: "+derivedHeader+", Value: "+ value+" ]";
        }
        public static List<AmortisationValueHolder> parsedValues(final String derivedHeader, final String evaluatedHeader, final List<String> values){
           final List<AmortisationValueHolder> amortValues = new LinkedList<>();
           if(values!= null && values.size()>0) {
               for (String value : values) {
                    amortValues.add(new AmortisationValueHolder(derivedHeader,evaluatedHeader,value));
               }
           }
           return amortValues;
        }

        @Override
        public boolean equals(Object obj) {
           if (obj == null || !(obj instanceof AmortisationValueHolder) ) return false;
           AmortisationValueHolder comparer = (AmortisationValueHolder) obj;
           return ((this.value==null && comparer.value==null) || this.value.equals(comparer.value))
                   &&
                   ((this.derivedHeader==null && comparer.derivedHeader==null) || this.derivedHeader.equals(comparer.derivedHeader))
                   &&
                   ((this.evaluatedHeader==null && comparer.evaluatedHeader==null) || this.evaluatedHeader.equals(comparer.evaluatedHeader));
        }

        @Override
        public int hashCode() {
            return this.value.hashCode()*131 + this.evaluatedHeader.hashCode()*131+this.derivedHeader.hashCode()*131;
        }
    }