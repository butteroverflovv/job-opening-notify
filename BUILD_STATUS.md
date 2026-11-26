# 0.1.2 검증 기록

- GitHub Actions: https://github.com/butteroverflovv/job-opening-notify/actions/runs/45843019763
- 앱 코드 커밋: 79b921094444e26f4212ce670ba19b72774b882a
- 기존 핵심 회귀 검사 60개 통과
- release APK 생성 / Android Lint 통과
- 개인 고정 키 서명 후 apksigner v2·v3 검증 통과
- 0.1.1 제공 APK와 인증서 SHA-256 일치 확인
- APK SHA-256: 1460d138d26fba29fbd1fa567fa181f36eed98580b89f64401dfc3b1203c694a

## 화면 변경

앱스 이름과 상단 제목을 ‘교원 채용공고 알리미’로 통일했습니다.
상단 지역 안내와 영문 소제목을 제거했습니다.
흰색 배경, 연회색 경계와 옅은 기본 elevation 그림자, 갈색 #7B533E 포인트를 적용했습니다.

아이콘 시안은 대화에서만 검토용으로 제공했습니다. 앱의 기존 ic_book 리소스와 manifest 아이콘 참조는 변경하지 않았습니다. 새 아이콘은 사용자 검토 후에 적용합니다.

실기기에서의 최종 화면 확인은 필요합니다. 0.1.1에서는 덮어 설치할 수 있고, 0.1.0에서는 앞서 안내한 백업 후 재설치가 필요합니다.
