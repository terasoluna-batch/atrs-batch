/*
 * Copyright (C) 2024 NTT Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package jp.co.ntt.atrs.batch.jobs;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.mybatis.spring.batch.builder.MyBatisCursorItemReaderBuilder;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.support.SingleItemPeekableItemReader;
import org.springframework.batch.item.support.builder.SingleItemPeekableItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.io.PathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ClassUtils;
import org.terasoluna.batch.item.file.transform.EnclosableDelimitedLineAggregator;

import jp.co.ntt.atrs.batch.common.listener.AtrsExceptionHandlingListener;
import jp.co.ntt.atrs.batch.common.listener.JobExitCodeChangeListener;
import jp.co.ntt.atrs.batch.common.listener.JobLoggingListener;
import jp.co.ntt.atrs.batch.config.JobBaseContextConfig;
import jp.co.ntt.atrs.batch.jbbb01002.DepartureDateChangeFieldExtractor;
import jp.co.ntt.atrs.batch.jbbb01002.JBBB01002Dao;
import jp.co.ntt.atrs.batch.jbbb01002.JBBB01002Tasklet;
import jp.co.ntt.atrs.batch.jbbb01002.RouteAggregationDto;
import jp.co.ntt.atrs.batch.jbbb01002.RouteAggregationResultDto;
import jp.co.ntt.atrs.batch.jbbb01002.WriteHeaderFlatFileHeaderCallback;

/**
 * JBBB01002用のJavaConfigクラス
 *
 * @author 電電次郎
 */
@Configuration
@Import(JobBaseContextConfig.class)
@ComponentScan(value = {"jp.co.ntt.atrs.batch.jbbb01002",
    "jp.co.ntt.atrs.batch.common.listener",
    "jp.co.ntt.atrs.batch.common.mapstruct"}, scopedProxy = ScopedProxyMode.TARGET_CLASS)
@MapperScan(basePackages = "jp.co.ntt.atrs.batch.jbbb01002", sqlSessionFactoryRef = "jobSqlSessionFactory")
public class JBBB01002Config {

    @Bean
    public MapperFactoryBean<JBBB01002Dao> JBBB01002Dao(
            SqlSessionTemplate batchModeSqlSessionTemplate) {
        MapperFactoryBean<JBBB01002Dao> dao = new MapperFactoryBean<>();
        dao.setMapperInterface(JBBB01002Dao.class);
        dao.setSqlSessionTemplate(batchModeSqlSessionTemplate);
        return dao;
    }
    
    @Bean
    public SingleItemPeekableItemReader<RouteAggregationResultDto> routeAggregationResultReader(
            MyBatisCursorItemReader<RouteAggregationResultDto> delegateReader) {
        return new SingleItemPeekableItemReaderBuilder<RouteAggregationResultDto>()
                .delegate(delegateReader)
                .build();
    }
    
    @Bean
    @StepScope
    public MyBatisCursorItemReader<RouteAggregationResultDto> delegateReader(
            @Value("#{stepExecutionContext['firstDate']}") Date firstDate,
            @Value("#{stepExecutionContext['lastDate']}") Date lastDate,
            @Qualifier("jobSqlSessionFactory") SqlSessionFactory jobSqlSessionFactory) {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("firstDate", firstDate);
        parameterValues.put("lastDate", lastDate);
        return new MyBatisCursorItemReaderBuilder<RouteAggregationResultDto>()
                .queryId("jp.co.ntt.atrs.batch.jbbb01002.JBBB01002Dao.findRouteAggregationByDepartureDateList")
                .sqlSessionFactory(jobSqlSessionFactory)
                .parameterValues(parameterValues)
                .build();
    }
    
    @Bean
    @StepScope
    public FlatFileItemWriter<RouteAggregationDto> routeAggregationWriter(
            @Value("${user.dir}") String userDir,
            @Value("${path.RouteAggregationData}") String pathRouteAggregationData,
            WriteHeaderFlatFileHeaderCallback writeHeaderFlatFileHeaderCallback) {
        EnclosableDelimitedLineAggregator<RouteAggregationDto> lineAggregator = new EnclosableDelimitedLineAggregator<>();
        lineAggregator.setDelimiter(',');
        lineAggregator.setEnclosure('"');
        lineAggregator.setAllEnclosing(true);
        DepartureDateChangeFieldExtractor fieldExtractor = new DepartureDateChangeFieldExtractor();
        lineAggregator.setFieldExtractor(fieldExtractor);
        return new FlatFileItemWriterBuilder<RouteAggregationDto>()
                .name(ClassUtils.getShortName(FlatFileItemWriter.class))
                .resource(new PathResource(userDir + File.separator + pathRouteAggregationData))
                .lineAggregator(lineAggregator)
                .encoding("UTF-8")
                .lineSeparator("\n")
                .append(true)
                .transactional(false)
                .headerCallback(writeHeaderFlatFileHeaderCallback)
                .build();
    }
    
    @Bean
    public Step step01(JobRepository jobRepository,
                       @Qualifier("jobResourcelessTransactionManager") PlatformTransactionManager transactionManager,
                       JBBB01002Tasklet tasklet,
                       AtrsExceptionHandlingListener atrsExceptionHandlingListener) {
        return new StepBuilder("JBBB01002.step01",
                jobRepository)
                .tasklet(tasklet, transactionManager)
                .listener(atrsExceptionHandlingListener)
                .build();
    }

    @Bean
    public Job JBBB01002(JobRepository jobRepository,
                                             Step step01,
                                             JobExitCodeChangeListener jobExitCodeChangeListener,
                                             JobLoggingListener jobLoggingListener) {
        return new JobBuilder("JBBB01002", jobRepository)
                .start(step01)
                .listener(jobExitCodeChangeListener)
                .listener(jobLoggingListener)
                .build();
    }
}
