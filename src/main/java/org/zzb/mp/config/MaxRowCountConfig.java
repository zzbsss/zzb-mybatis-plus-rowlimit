package org.zzb.mp.config;

import java.util.ArrayList;
import java.util.List;

public class MaxRowCountConfig {

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 定时更新监控表 cron 表达式
     * 默认 每天凌晨1点执行
     */
    private String execCron = "0 0 1 * * ?";

    /**
     * 最大行数限制，默认5万
     */
    private int maxRowCount = 50000;

    /**
     * 需要检查表的阈值,默认10万
     */
    private long checkTableSize = 100000;

    /**
     * 需要检查的数据库标识
     */
    private List<String> checkDataSources = new ArrayList<>();

    /**
     * 需要检查的表名
     */
    private List<String> checkTables = new ArrayList<>();

    /**
     * 超过限制提示
     */
    private String errorMessage = "超过最大查询数量限制，本次查询中断";

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getExecCron() {
        return execCron;
    }

    public void setExecCron(String execCron) {
        this.execCron = execCron;
    }

    public int getMaxRowCount() {
        return maxRowCount;
    }

    public void setMaxRowCount(int maxRowCount) {
        this.maxRowCount = maxRowCount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCheckTableSize() {
        return checkTableSize;
    }

    public void setCheckTableSize(long checkTableSize) {
        this.checkTableSize = checkTableSize;
    }

    public List<String> getCheckDataSources() {
        return checkDataSources;
    }

    public void setCheckDataSources(List<String> checkDataSources) {
        this.checkDataSources = checkDataSources;
    }

    public List<String> getCheckTables() {
        return checkTables;
    }

    public void setCheckTables(List<String> checkTables) {
        this.checkTables = checkTables;
    }
}
