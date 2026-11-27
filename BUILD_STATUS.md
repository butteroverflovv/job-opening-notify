# 0.1.13 검증 기록

- 코드: `b8d1fe850fffa9960d5413c72f1b14dfcb01bace`
- 빌드: https://github.com/butteroverflovv/job-opening-notify/actions/runs/45957571544 (성공)
- 핵심 검사 251개, release APK 빌드·Android Lint 통과.
- 사용자가 휴대폰에서 저장한 원본 목록·본문 HTML 전체를 로컬 검사: 중복 제외 공고 17건, 게시일 17건, 키워드·학년도·게시일 조건 일치 1건, 본문 557자.
- 원본 HTML은 수정하거나 저장소에 올리지 않음. 재현 검사에는 최소한의 공개 공고 구조·샘플만 사용.
- 고양외고의 boardView/boardPage에서 숫자 글 번호·페이지를 해석하고, categoryid=12/menuid=03/groupid=00/boardtable=alim의 list/view만 POST로 전송.
- 쓰기·수정·삭제·임의 mode는 네트워크 요청 전에 거절. 첨부파일명·비밀번호 등 입력은 전달하지 않음.
- 독립 JVM의 HTTP 연결 대역으로 실제 Net 호출의 POST·폼 인코딩·논리 공고 주소 유지 및 타 게시판 GET 유지 검증.
- 고정 공지·글 주소 중복·제목 키워드·게시일 판정 및 noticeview/content 본문 셀 추출 검사. 첨부파일 행은 제외.
- 원문은 POST 지원 WebView로 열며 외부 브라우저에서 학교 게시판을 여는 버튼 제공.
- Android API 29 일반 화면(411dp/글자 100%) 및 작은 화면(320dp/글자 130%) UI 검사 통과. 고양 원문이 POST 지원 화면으로 연결되는 경로 포함.
- 확인 환경에서는 새 POST 요청도 HTTP 400을 반환하여 실시간 서버 연결 성공은 확인하지 못함. 사용자는 휴대폰 브라우저·진단 화면에서 정상 접근하고 HTML을 제공했음. 실제 기기 수집 결과는 업데이트 후 확인 필요.

## 서명된 APK

- 파일: `history-jobs-0.1.13.apk`, versionCode 14
- 크기: 2,768,496 bytes
- SHA-256: `7c6e8d694769c6710363666d5b39be67ae8cc8f6224c7fb66402ffd12aa26a21`
- v2/v3 서명 검증 통과. 기존 앱 ID·고정 서명 유지.
- 인증서 SHA-256: `9aa4d4d7223d7f22b68592ce7f4b37422dbd202790549443041c93f0f32947d2`
- release DEX의 새 폼 처리·버전 및 테스트 전용 코드 제외 확인.
