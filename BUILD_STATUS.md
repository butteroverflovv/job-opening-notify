# 빌드 및 검증 기록

작성일: 2026-11-26 (한국 시간)

- 핵심 Java 모듈: 컴파일 성공
- 합성 입력 회귀 검사: 50개 통과
- Android APK: **미생성**
- 실제 게시판 수집 / 실기기 실행: **미검증**

## 빌드 시도

`./gradlew --no-daemon :app:assembleDebug`

Gradle 8.11.1 배포 파일 다운로드 단계에서 `java.net.SocketException: Network is unreachable`로 중단됐습니다. Android SDK도 이 환경에 없습니다. 이 오류는 앱의 컴파일 성공·실패를 판정한 결과가 아닙니다.

## 사이트 직접 연결 시도

세 게시판 모두 이 작업 환경의 HTTP 요청에서 403을 반환했습니다. 사이트 자체가 모든 접속을 막았다고 단정할 수 없으며, 휴대폰에서의 요청은 별도 검증이 필요합니다.

## 다음 검증

1. GitHub Actions 또는 Android Studio에서 앱 전체 컴파일·Lint
2. APK 설치 후 사이트별 목록/상세/첨부 수집
3. 알림 권한, 테스트 알림, 한국 시간 두 번 예약
4. 재부팅·절전 후 동작 확인
