/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.minilang.method.otherops;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import org.apache.ofbiz.base.util.UtilXml;
import org.apache.ofbiz.base.util.collections.FlexibleMapAccessor;
import org.apache.ofbiz.base.util.string.FlexibleStringExpander;
import org.apache.ofbiz.minilang.MiniLangElement;
import org.apache.ofbiz.minilang.MiniLangException;
import org.apache.ofbiz.minilang.MiniLangRuntimeException;
import org.apache.ofbiz.minilang.MiniLangUtil;
import org.apache.ofbiz.minilang.MiniLangValidate;
import org.apache.ofbiz.minilang.SimpleMethod;
import org.apache.ofbiz.minilang.method.MethodContext;
import org.apache.ofbiz.minilang.method.MethodOperation;
import org.w3c.dom.Element;

/**
 * Implements the &lt;calculate&gt; element.
 * 
 * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
 */
public final class Calculate extends MethodOperation {

    public static final String module = Calculate.class.getName();

    public static final int TYPE_DOUBLE = 1;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_LONG = 3;
    public static final int TYPE_INTEGER = 4;
    public static final int TYPE_STRING = 5;
    public static final int TYPE_BIG_DECIMAL = 6;

    private final Calculate.SubCalc calcops[];
    private final FlexibleStringExpander decimalFormatFse;
    private final FlexibleStringExpander decimalScaleFse;
    private final FlexibleMapAccessor<Object> fieldFma;
    private final FlexibleStringExpander roundingModeFse;
    private final int type;
    private final String typeString;

    public Calculate(Element element, SimpleMethod simpleMethod) throws MiniLangException {
        super(element, simpleMethod);
        if (MiniLangValidate.validationOn()) {
            MiniLangValidate.handleError("<calculate> element is deprecated (use <set>)", simpleMethod, element);
            MiniLangValidate.attributeNames(simpleMethod, element, "field", "decimal-scale", "decimal-format", "rounding-mode", "type");
            MiniLangValidate.requiredAttributes(simpleMethod, element, "field");
            MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
            MiniLangValidate.childElements(simpleMethod, element, "calcop", "number");
        }
        this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
        this.decimalFormatFse = FlexibleStringExpander.getInstance(element.getAttribute("decimal-format"));
        this.decimalScaleFse = FlexibleStringExpander.getInstance(element.getAttribute("decimal-scale"));
        this.roundingModeFse = FlexibleStringExpander.getInstance(element.getAttribute("rounding-mode"));
        this.typeString = element.getAttribute("type");
        int type = Calculate.TYPE_BIG_DECIMAL;
        if ("Double".equals(typeString)) {
            type = Calculate.TYPE_DOUBLE;
        } else if ("Float".equals(typeString)) {
            type = Calculate.TYPE_FLOAT;
        } else if ("Long".equals(typeString)) {
            type = Calculate.TYPE_LONG;
        } else if ("Integer".equals(typeString)) {
            type = Calculate.TYPE_INTEGER;
        } else if ("String".equals(typeString)) {
            type = Calculate.TYPE_STRING;
        } else if ("BigDecimal".equals(typeString)) {
            type = Calculate.TYPE_BIG_DECIMAL;
        }
        this.type = type;
        List<? extends Element> calcopElements = UtilXml.childElementList(element);
        calcops = new Calculate.SubCalc[calcopElements.size()];
        int i = 0;
        for (Element calcopElement : calcopElements) {
            String nodeName = calcopElement.getNodeName();
            if ("calcop".equals(nodeName)) {
                calcops[i] = new CalcOp(calcopElement, simpleMethod);
            } else if ("number".equals(nodeName)) {
                calcops[i] = new NumberOp(calcopElement, simpleMethod);
            } else {
                MiniLangValidate.handleError("Invalid calculate sub-element.", simpleMethod, calcopElement);
                calcops[i] = new InvalidOp(calcopElement, simpleMethod);
            }
            i++;
        }
    }

    @Override
    public boolean exec(MethodContext methodContext) throws MiniLangException {
        String roundingModeString = roundingModeFse.expandString(methodContext.getEnvMap());
        RoundingMode roundingMode = RoundingMode.HALF_EVEN;
        if ("Ceiling".equals(roundingModeString)) {
            roundingMode = RoundingMode.CEILING;
        } else if ("Floor".equals(roundingModeString)) {
            roundingMode = RoundingMode.FLOOR;
        } else if ("Up".equals(roundingModeString)) {
            roundingMode = RoundingMode.UP;
        } else if ("Down".equals(roundingModeString)) {
            roundingMode = RoundingMode.DOWN;
        } else if ("HalfUp".equals(roundingModeString)) {
            roundingMode = RoundingMode.HALF_UP;
        } else if ("HalfDown".equals(roundingModeString)) {
            roundingMode = RoundingMode.HALF_DOWN;
        } else if ("Unnecessary".equals(roundingModeString)) {
            roundingMode = RoundingMode.UNNECESSARY;
        }
        String decimalScaleString = decimalScaleFse.expandString(methodContext.getEnvMap());
        int decimalScale = 2;
        if (!decimalScaleString.isEmpty()) {
            decimalScale = Integer.valueOf(decimalScaleString);
        }
        BigDecimal resultValue = BigDecimal.ZERO.setScale(decimalScale, roundingMode);
        for (Calculate.SubCalc calcop : calcops) {
            resultValue = resultValue.add(calcop.calcValue(methodContext, decimalScale, roundingMode));
        }
        resultValue = resultValue.setScale(decimalScale, roundingMode);
        Object resultObj = null;
        switch (type) {
            case TYPE_DOUBLE:
                resultObj = resultValue.doubleValue();
                break;
            case TYPE_FLOAT:
                resultObj = resultValue.floatValue();
                break;
            case TYPE_LONG:
                resultValue = resultValue.setScale(0, roundingMode);
                resultObj = resultValue.longValue();
                break;
            case TYPE_INTEGER:
                resultValue = resultValue.setScale(0, roundingMode);
                resultObj = resultValue.intValue();
                break;
            case TYPE_STRING:
                // run the decimal-formatting
                String decimalFormatString = decimalFormatFse.expandString(methodContext.getEnvMap());
                DecimalFormat df = null;
                if (!decimalFormatString.isEmpty()) {
                    df = new DecimalFormat(decimalFormatString);
                }
                if (df != null && resultValue.compareTo(BigDecimal.ZERO) != 0) {
                    resultObj = df.format(resultValue);
                } else {
                    resultObj = resultValue.toString();
                }
                break;
            case TYPE_BIG_DECIMAL:
                resultObj = resultValue;
                break;
        }
        fieldFma.put(methodContext.getEnvMap(), resultObj);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<set ");
        sb.append("field=\"").append(this.fieldFma).append("\" ");
        if (!this.roundingModeFse.isEmpty()) {
            sb.append("rounding-mode=\"").append(this.roundingModeFse).append("\" ");
        }
        if (!this.decimalScaleFse.isEmpty()) {
            sb.append("decimal-scale=\"").append(this.decimalScaleFse).append("\" ");
        }
        if (!this.decimalFormatFse.isEmpty()) {
            sb.append("decimal-format=\"").append(this.decimalFormatFse).append("\" ");
        }
        if (!typeString.isEmpty()) {
            sb.append("type=\"").append(this.typeString).append("\" ");
        }
        sb.append("/>");
        return sb.toString();
    }

    /**
     * Interface for &lt;calculate&gt; sub-element implementations.
     */
    public interface SubCalc {
        BigDecimal calcValue(MethodContext methodContext, int scale, RoundingMode roundingMode) throws MiniLangException;
    }

    /**
     * Implements the &lt;calcop&gt; element.
     * 
     * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
     */
    public final class CalcOp extends MiniLangElement implements SubCalc {
        private static final int OPERATOR_ADD = 1;
        private static final int OPERATOR_DIVIDE = 4;
        private static final int OPERATOR_MULTIPLY = 3;
        private static final int OPERATOR_NEGATIVE = 5;
        private static final int OPERATOR_SUBTRACT = 2;

        private final Calculate.SubCalc calcops[];
        private final FlexibleMapAccessor<Object> fieldFma;
        private final int operator;

        private CalcOp(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            super(element, simpleMethod);
            if (MiniLangValidate.validationOn()) {
                MiniLangValidate.attributeNames(simpleMethod, element, "field", "operator");
                MiniLangValidate.requiredAttributes(simpleMethod, element, "operator");
                MiniLangValidate.expressionAttributes(simpleMethod, element, "field");
                MiniLangValidate.childElements(simpleMethod, element, "calcop", "number");
            }
            this.fieldFma = FlexibleMapAccessor.getInstance(element.getAttribute("field"));
            String operatorStr = element.getAttribute("operator");
            int operator = CalcOp.OPERATOR_ADD;
            if ("subtract".equals(operatorStr)) {
                operator = CalcOp.OPERATOR_SUBTRACT;
            } else if ("multiply".equals(operatorStr)) {
                operator = CalcOp.OPERATOR_MULTIPLY;
            } else if ("divide".equals(operatorStr)) {
                operator = CalcOp.OPERATOR_DIVIDE;
            } else if ("negative".equals(operatorStr)) {
                operator = CalcOp.OPERATOR_NEGATIVE;
            }
            this.operator = operator;
            List<? extends Element> calcopElements = UtilXml.childElementList(element);
            calcops = new Calculate.SubCalc[calcopElements.size()];
            int i = 0;
            for (Element calcopElement : calcopElements) {
                if ("calcop".equals(calcopElement.getNodeName())) {
                    calcops[i] = new Calculate.CalcOp(calcopElement, simpleMethod);
                } else if ("number".equals(calcopElement.getNodeName())) {
                    calcops[i] = new Calculate.NumberOp(calcopElement, simpleMethod);
                } else {
                    MiniLangValidate.handleError("Invalid calculate sub-element.", simpleMethod, calcopElement);
                    calcops[i] = new InvalidOp(calcopElement, simpleMethod);
                }
                i++;
            }
        }

        @Override
        public BigDecimal calcValue(MethodContext methodContext, int scale, RoundingMode roundingMode) throws MiniLangException {
            BigDecimal resultValue = BigDecimal.ZERO.setScale(scale, roundingMode);
            boolean isFirst = true;
            Object fieldObj = fieldFma.get(methodContext.getEnvMap());
            if (fieldObj != null) {
                if (fieldObj instanceof Double) {
                    resultValue = new BigDecimal((Double) fieldObj);
                } else if (fieldObj instanceof Long) {
                    resultValue = BigDecimal.valueOf((Long) fieldObj);
                } else if (fieldObj instanceof Float) {
                    resultValue = new BigDecimal((Float) fieldObj);
                } else if (fieldObj instanceof Integer) {
                    resultValue = BigDecimal.valueOf(((Integer) fieldObj).longValue());
                } else if (fieldObj instanceof String) {
                    resultValue = new BigDecimal((String) fieldObj);
                } else if (fieldObj instanceof BigDecimal) {
                    resultValue = (BigDecimal) fieldObj;
                }
                if (operator == OPERATOR_NEGATIVE)
                    resultValue = resultValue.negate();
                isFirst = false;
            }
            for (SubCalc calcop : calcops) {
                if (isFirst) {
                    resultValue = calcop.calcValue(methodContext, scale, roundingMode);
                    if (operator == OPERATOR_NEGATIVE)
                        resultValue = resultValue.negate();
                    isFirst = false;
                } else {
                    switch (operator) {
                        case OPERATOR_ADD:
                            resultValue = resultValue.add(calcop.calcValue(methodContext, scale, roundingMode));
                            break;
                        case OPERATOR_SUBTRACT:
                        case OPERATOR_NEGATIVE:
                            resultValue = resultValue.subtract(calcop.calcValue(methodContext, scale, roundingMode));
                            break;
                        case OPERATOR_MULTIPLY:
                            resultValue = resultValue.multiply(calcop.calcValue(methodContext, scale, roundingMode));
                            break;
                        case OPERATOR_DIVIDE:
                            resultValue = resultValue.divide(calcop.calcValue(methodContext, scale, roundingMode), scale, roundingMode);
                            break;
                    }
                }
            }
            return resultValue;
        }
    }

    /**
     * Implements the &lt;number&gt; element.
     * 
     * @see <a href="https://cwiki.apache.org/confluence/display/OFBIZ/Mini+Language+-+minilang+-+simple-method+-+Reference">Mini-language Reference</a>
     */
    public final class NumberOp extends MiniLangElement implements SubCalc {

        private final FlexibleStringExpander valueFse;

        private NumberOp(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            super(element, simpleMethod);
            if (MiniLangValidate.validationOn()) {
                MiniLangValidate.attributeNames(simpleMethod, element, "value");
                MiniLangValidate.requiredAttributes(simpleMethod, element, "value");
                MiniLangValidate.noChildElements(simpleMethod, element);
            }
            valueFse = FlexibleStringExpander.getInstance(element.getAttribute("value"));
        }

        @Override
        public BigDecimal calcValue(MethodContext methodContext, int scale, RoundingMode roundingMode) throws MiniLangException {
            String valueStr = valueFse.expandString(methodContext.getEnvMap());
            Locale locale = methodContext.getLocale();
            if (locale == null)
                locale = Locale.getDefault();
            try {
                BigDecimal parsedVal = (BigDecimal) MiniLangUtil.convertType(valueStr, java.math.BigDecimal.class, locale, null, null);
                return parsedVal.setScale(scale, roundingMode);
            } catch (Exception e) {
                throw new MiniLangRuntimeException("Exception thrown while parsing value attribute: " + e.getMessage(), this);
            }
        }
    }

    private final class InvalidOp extends MiniLangElement implements SubCalc {

        private InvalidOp(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            super(element, simpleMethod);
        }

        @Override
        public BigDecimal calcValue(MethodContext methodContext, int scale, RoundingMode roundingMode) throws MiniLangException {
            throw new MiniLangRuntimeException("Invalid calculate sub-element.", this);
        }
    }

    /**
     * A factory for the &lt;calculate&gt; element.
     */
    public static final class CalculateFactory implements Factory<Calculate> {
        @Override
        public Calculate createMethodOperation(Element element, SimpleMethod simpleMethod) throws MiniLangException {
            return new Calculate(element, simpleMethod);
        }

        @Override
        public String getName() {
            return "calculate";
        }
    }
}
