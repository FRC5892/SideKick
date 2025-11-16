# Turn on

- Install dependencies once:
  pip install -r bayesopt/tuner/requirements.txt

- Enable tuner:
  Set TUNER_ENABLED = True and set TEAM_NUMBER or NT_SERVER_IP.

- Start (if not autostarted):
  cd bayesopt/scripts && python3 tuner_daemon.py

Quick checks:
- Tail log: tail -n 200 tuner_logs/tuner_daemon.log
- Verify CSVs: ls tuner_logs/bayesian_tuner_*.csv
