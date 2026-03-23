package com.nowcoder.community.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quartz 示例任务。
 */
public class AlphaJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlphaJob.class);

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        LOGGER.info("{}: execute a quartz job.", Thread.currentThread().getName());
    }
}
