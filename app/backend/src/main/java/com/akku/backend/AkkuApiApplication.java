package com.akku.backend;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Slf4j
@EnableJpaAuditing
@SpringBootApplication
public class AkkuApiApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(AkkuApiApplication.class, args);
	}

	/**
	 * working directory부터 최대 4단계 상위까지 .env 파일을 탐색하여 로드.
	 * IntelliJ의 working directory 설정과 무관하게 동작한다.
	 */
	private static void loadDotEnv() {
		java.nio.file.Path dir = java.nio.file.Paths.get(System.getProperty("user.dir"));
		for (int i = 0; i < 4; i++) {
			if (dir.resolve(".env").toFile().exists()) {
				try {
					Dotenv dotenv = Dotenv.configure().directory(dir.toString()).load();
					dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
				} catch (DotenvException e) {
					log.warn(".env 파일 파싱 실패 — 환경변수 없이 기동을 계속합니다. 경로={}, 원인={}", dir, e.getMessage());
				}
				return;
			}
			dir = dir.getParent();
			if (dir == null) break;
		}
	}

}
