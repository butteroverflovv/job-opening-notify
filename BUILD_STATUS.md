# 0.1.4 검증 기록

Montage의 구성 요소를 참고한 네이티브 화면 개편입니다. 설치용 APK는 기존 고정 개인 키로 서명했습니다.

- 코드 커밋: 3097fff4eab32bfd6272320a0507cf6dbe3dc0cd
- GitHub Actions: https://github.com/butteroverflovv/job-opening-notify/actions/runs/45848548500
- release APK 빌드와 Android Lint 통과.
- 제목 필터·수집 등 핵심 검사 60개 통과.
- API 29 에뮬레이터에서 기본 411dp/글자 100% 및 작은 320dp/글자 130% 환경의 UI 검사 모두 통과.
- UI 검사: 공고 표시, 관심 저장 및 필터, 검색과 검색어 지우기, 마감 포함, 상세 창 열기/닫기, 하단 메뉴, 게시판 스위치, 예약 스위치. 제목과 소개 문구의 줄바꿈 및 주요 제어의 표시 영역도 확인.
- 두 환경의 스크린샷 각 8장과 검사 로그는 Actions의 history-jobs-ui-check artifact에 보관합니다. 테스트 공고와 실행기는 release APK에 포함되지 않음을 확인했습니다.
- 승인 아이콘 PNG의 픽셀이 APK 내부에서도 원본과 전부 같음을 확인.
- APK 크기: 2743864 bytes
- APK SHA-256: c47b38624f43c18bc995683846d934ea1417423451007b3a681a63b4000705d3
- APK v2·v3 서명 검증 통과. 인증서 SHA-256: 9aa4d4d7223d7f22b68592ce7f4b37422dbd202790549443041c93f0f32947d2
- 데이터 형식과 수집·알림 로직은 유지했습니다. 이전 0.1.1 이후 설치본과 같은 서명입니다.
- 실제 사용자 기기에서의 백그라운드 예약·알림 및 Android 15 edge-to-edge 화면은 기기별 확인이 필요합니다.
