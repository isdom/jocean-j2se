package org.jocean.j2se.prometheus;

import java.io.IOException;
import java.io.Writer;

import io.prometheus.client.Collector;

public class TextFormatUtil {
    /**
     * Content-type for text version 0.0.4.
     */
    public final static String CONTENT_TYPE_004 = "text/plain; version=0.0.4; charset=utf-8";

    /**
     * Write out the text version 0.0.4 of the given MetricFamilySamples.
     */
    public static void write004(final Writer writer, final Collector.MetricFamilySamples metricFamilySamples, final String...commonLabels)
            throws IOException {
        /*
         * See http://prometheus.io/docs/instrumenting/exposition_formats/ for
         * the output format specification.
         */
        writer.write("# HELP ");
        writer.write(metricFamilySamples.name);
        writer.write(' ');
        writeEscapedHelp(writer, metricFamilySamples.help);
        writer.write('\n');

        writer.write("# TYPE ");
        writer.write(metricFamilySamples.name);
        writer.write(' ');
        writer.write(typeString(metricFamilySamples.type));
        writer.write('\n');

        for (final Collector.MetricFamilySamples.Sample sample : metricFamilySamples.samples) {
            writer.write(sample.name);
            if (sample.labelNames.size() + commonLabels.length > 0) {
                writer.write('{');
                for (int i = 0; i < sample.labelNames.size(); ++i) {
                    writer.write(sample.labelNames.get(i));
                    writer.write("=\"");
                    writeEscapedLabelValue(writer, sample.labelValues.get(i));
                    writer.write("\",");
                }
                for (int i = 0; i < commonLabels.length - 1; i+=2) {
                    writer.write(commonLabels[i]);
                    writer.write("=\"");
                    writeEscapedLabelValue(writer, commonLabels[i+1]);
                    writer.write("\",");
                }
                writer.write('}');
            }
            writer.write(' ');
            writer.write(Collector.doubleToGoString(sample.value));
            if (sample.timestampMs != null) {
                writer.write(' ');
                writer.write(sample.timestampMs.toString());
            }
            writer.write('\n');
        }
    }

    private static void writeEscapedHelp(final Writer writer, final String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
            case '\\':
                writer.append("\\\\");
                break;
            case '\n':
                writer.append("\\n");
                break;
            default:
                writer.append(c);
            }
        }
    }

    private static void writeEscapedLabelValue(final Writer writer, final String s) throws IOException {
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
            case '\\':
                writer.append("\\\\");
                break;
            case '\"':
                writer.append("\\\"");
                break;
            case '\n':
                writer.append("\\n");
                break;
            default:
                writer.append(c);
            }
        }
    }

    private static String typeString(final Collector.Type t) {
        switch (t) {
        case GAUGE:
            return "gauge";
        case COUNTER:
            return "counter";
        case SUMMARY:
            return "summary";
        case HISTOGRAM:
            return "histogram";
        default:
            return "untyped";
        }
    }
}
