package com.bytedance.tools.codelocator.model;

import com.bytedance.tools.codelocator.utils.Log;

import javax.swing.table.AbstractTableModel;
import java.net.URLEncoder;
import java.util.LinkedList;
import java.util.List;

public class EditableTableModel extends AbstractTableModel {

    private List<ArgInfo> argInfos = new LinkedList<>();

    public EditableTableModel() {
        addRow(0);
    }

    public void addRow(int index) {
        if (argInfos.size() >= index) {
            argInfos.add(index, new ArgInfo());
        }
    }

    public void clearAll() {
        argInfos.clear();
        addRow(0);
    }

    public String maxLengthKey() {
        String key = "";
        for (ArgInfo argInfo : argInfos) {
            if (argInfo.getKey().length() > key.length()) {
                key = argInfo.getKey();
            }
        }
        return key;
    }

    public String buildArgsStr() {
        StringBuilder sb = new StringBuilder();
        for (ArgInfo argInfo : argInfos) {
            if (argInfo.isEnabled() && argInfo.getKey() != null && !argInfo.getKey().isEmpty() && argInfo.getValue() != null) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(argInfo.getKey());
                sb.append("=");
                try {
                    sb.append(URLEncoder.encode(argInfo.getValue(), "UTF-8"));
                } catch (Throwable t) {
                    sb.append(argInfo.getValue());
                    Log.e("Encode arg error " + argInfo.getValue(), t);
                }
            }
        }
        if (sb.length() > 0) {
            sb.insert(0, "?");
        }
        return sb.toString();
    }

    public void addArgs(String key, String value) {
        for (int i = 0; i < argInfos.size(); i++) {
            ArgInfo info = argInfos.get(i);
            if (key != null && key.equals(info.getKey()) ||
                    ((info.getKey() == null || info.getKey().isEmpty()) && (info.getValue() == null || info.getValue().isEmpty()))) {
                info.setKey(key);
                info.setValue(value);
                info.setEnabled(true);
                if (i == argInfos.size() - 1) {
                    addRow(argInfos.size());
                }
                return;
            }
        }
        argInfos.add(new ArgInfo(key, value));
    }

    public void removeRow(int index) {
        if (argInfos.size() > index) {
            if (argInfos.size() == 1) {
                argInfos.get(0).setEnabled(false);
                argInfos.get(0).setKey("");
                argInfos.get(0).setValue("");
            } else {
                argInfos.remove(index);
            }
        }
    }

    @Override
    public int getRowCount() {
        return argInfos.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 1:
                return "key";
            case 2:
                return "value";
            default:
                return "enable";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Boolean.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 0:
                return argInfos.get(rowIndex).isEnabled();
            case 1:
                return argInfos.get(rowIndex).getKey();
            case 2:
                return argInfos.get(rowIndex).getValue();
        }
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        switch (columnIndex) {
            case 1:
                argInfos.get(rowIndex).setEnabled(true);
                argInfos.get(rowIndex).setKey(((String) aValue).trim());
                break;
            case 2:
                argInfos.get(rowIndex).setValue((String) aValue);
                break;
            default:
                argInfos.get(rowIndex).setEnabled((Boolean) aValue);
                break;
        }
        if ((rowIndex == argInfos.size() - 1) && argInfos.get(rowIndex).getKey() != null && !argInfos.get(rowIndex).getKey().isEmpty() &&
                argInfos.get(rowIndex).getValue() != null && !argInfos.get(rowIndex).getValue().isEmpty()) {
            addRow(argInfos.size());
        }
    }
}
