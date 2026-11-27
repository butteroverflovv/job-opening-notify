# 0.1.20 검증 기록

코드: 89d8e4048e7a362b164802c8683abecd186cf87c
빌드: https://github.com/butteroverflovv/job-opening-notify/actions/runs/45996899581 (성공)
버전: 0.1.20 / versionCode 21. 기존 applicationId·서명 유지.

## 변경

공고 탭의 최근 확인순을 최신 게시순으로 변경. 게시일을 정규화한 YYYY-MM-DD의 내림차순으로 정렬하며 같은 게시일은 기존 firstSeen 내림차순 및 ID로 안정적으로 정렬한다. 누락 게시일은 정상 날짜 뒤로 보낸다(기존 공고 표시 조건상 누락 게시일 공고는 제외됨). 마감 가까운 순은 기존과 같다.

내부 UI 변수는 noticeNewest로 바꾸고 저장 키 noticeRecent는 이어 사용해 기존 최근 확인순 선택이 새 최신 게시순으로 바로 연결되도록 한다. 버튼·대화상자·접근성 설명을 모두 최신 게시순으로 바꾸고 필터/검색 후 선택을 유지한다. 수집·저장 데이터와 캐시는 변경하지 않으며 재수집 없이 저장 공고에 적용한다.

## 검증

- 핵심 검사 398개 통과. 게시일과 발견 시각이 반대인 예시로 게시일 우선 정렬 확인. 동일 게시일 보조 정렬과 누락 날짜 처리도 확인.
- Android normal / compact-large-text 각각 OK (1 test). preview0은 어제 게시·더 늦게 발견, preview1은 오늘 게시·더 일찍 발견하도록 설정한 뒤 최신 게시순에서 preview1이 위에 오는지 확인. 마감 순 원복, 선택 저장, 필터 후 유지, 새 표시 문구 확인.
- 기존 중복 통합·초등 제외·키워드·본문·캐시·소요 시간·알림 검사도 통과.
- release 빌드 및 Lint 성공.
- release dex에서 0.1.20/NoticeOrder/notice_sort/최신 게시순 확인. 최근 확인순 문구 및 테스트 러너/서버 미포함.
- 서명 v2/v3 검증. 기존 인증서 SHA256: 9aa4d4d7223d7f22b68592ce7f4b37422dbd202790549443041c93f0f32947d2
- APK: history-jobs-0.1.20.apk / 2784880 bytes
- APK SHA256: 648e8432af8befa37ca32e1499c765856afba4f115b0b01799dedc65420b0162
