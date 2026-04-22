 
Requirements Analysis Document
BarberBook – Gestionale di Hair Man Tony



Riferimento	2026_Tirocinio_RAD
Versione	1.0
Data	20/04/2025
Destinatario	Vincenzo Liguorino
Presentato da	Gianfranco Barba
Approvato da	//

 

Sommario
1	Introduzione	5
1.1	Obiettivo del sistema	5
1.2	Ambito del sistema	5
1.3	Obiettivi e criteri di successo del sistema	5
1.4	Definizioni, acronimi e abbreviazioni	6
1.5	Riferimenti	6
1.6	Organizzazione del documento	7
2	Sistema Corrente	7
2.1	Analisi del dominio	7
2.2	Flussi di lavoro attuali	7
2.3	Problemi e Criticità	7
3	Sistema Proposto	8
3.1	Panoramica del sistema	8
3.2	Requisiti Funzionali (RF)	8
3.2.1	Requisiti Generali (RF_GEN)	8
3.2.2	Lato Barbiere (RF_BAR)	8
3.2.3	Lato Cliente - Utente Generico (RF_CLI)	10
3.2.4	Lato Cliente - Solo Registrato (RF_CLR)	11
3.2.5	Lato Cliente - Solo Ospite (RF_CLG)	12
3.3	Requisiti Non Funzionali (RNF)	12
3.3.1	Usabilità (Usability)	12
3.3.2	Affidabilità (Reliability)	12
3.3.3	Prestazioni (Performance)	13
3.3.4	Supportabilità (Supportability)	13
3.3.5	Pseudo-requisiti (+)	13
3.4	Scenari	13
3.4.1	Scenario 1 — Gestione dell'agenda del giorno	13
3.4.2	Scenario 2 — Ricezione e gestione di una richiesta cliente	13
3.4.3	Scenario 3 — Prenotazione da parte di un cliente ospite	14
3.4.4	Scenario 4 — Annullamento di una prenotazione da parte del cliente	14
3.4.5	Scenario 5 — Configurazione iniziale del salone	14
3.5	Use Cases	15
3.5.1	UC_BAR_1 — GestioneAgendaGiornaliera	15
3.5.2	UC_BAR_2 — GestioneRichiestaDiPrenotazione	16
3.5.3	UC_CLI_1 — InvioRichiestaPrenotazione	17
3.5.4	UC_CLR_1 — GestioneProfiloEStorico	18
3.6	Modello ad Oggetti	19
3.6.1	Entity Objects	20
3.6.2	Boundary Objects	21
3.6.3	Control Objects	22
3.6.4	Riepilogo delle classi	22
3.7	Modello Dinamico	23
3.7.1	SD_1 — Invio e gestione di una richiesta di prenotazione (CLR → BAR)	23
3.7.2	SD_2 — Creazione diretta di una prenotazione da gestionale (BAR)	25
4	Glossario	27

 
 
Revision History
Data	Versione	Descrizione	Autori
10/04/2026	0.1	Prima stesura - Introduzione, attori, requisiti funzionali	GB
15/04/2026	0.2	Seconda Stesura – Correzione requisiti funzionali e aggiunta requisiti non funzionali, scenari	GB
15/04/2026	0.3	Terza Stesura – Aggiunta degli Use Cases	GB
17/04/2026	0.4	Quarta Stesura – Aggiunta Modello ad Oggetti	GB
20/04/2026	0.5	Quinta stesura – Modello Dinamico 	GB
21/04/2026	0.6	Sesta stesura – Aggiunta Sequence Diagram al Modello Dinamico	GB
21/04/2026	0.7	Settima stesura – Aggiunta Glossario	GB
23/06/2026	1.0	Revisione finale e consegna	GB
Project Manager
Nome e Cognome	Acronimo	E-mail
Gianfranco Barba	GB	g.barba14@studenti.unisa.it


 
1	Introduzione
1.1	Obiettivo del sistema
Il sistema che si intende realizzare, denominato BarberBook, ha come scopo principale la digitalizzazione e la semplificazione della gestione delle prenotazioni di un salone di barbiere. Attualmente il barbiere amministra gli appuntamenti tramite fogli Excel condivisi, con evidenti problemi di sincronizzazione tra i dispositivi utilizzati (PC, tablet, smartphone) e difficoltà di coordinamento delle informazioni.

In particolare, il sistema si propone di:
•	Fornire al barbiere un gestionale centralizzato e accessibile da qualsiasi dispositivo, che elimini la necessità di file condivisi e sincronizzazioni manuali.
•	Consentire al barbiere di configurare liberamente le proprie poltrone, i servizi offerti e gli orari di lavoro, rendendo il sistema adattabile alle specifiche esigenze del salone.
•	Offrire ai clienti — sia registrati che ospiti — la possibilità di visualizzare la disponibilità in tempo reale e inviare richieste di prenotazione, con un processo di convalida sempre in capo al barbiere.
•	Garantire al barbiere il pieno controllo sul flusso delle prenotazioni: nessuna prenotazione diventa confermata senza la sua esplicita approvazione.
1.2	Ambito del sistema
BarberBook è una piattaforma web-based responsive, progettata per funzionare in modo ottimale su PC, tablet e smartphone senza necessità di installazione. Il sistema gestisce un salone con due poltrone operative, ma la sua architettura è predisposta per supportare un numero variabile di poltrone configurabili dall'amministratore.

Le principali macro-funzionalità incluse nel sistema sono:
•	Gestione del catalogo servizi: il barbiere crea, modifica e rimuove i servizi offerti (es. taglio, barba, trattamenti) in modo autonomo e dinamico.
•	Gestione delle poltrone: aggiunta, rimozione e personalizzazione del nome di ciascuna poltrona disponibile nel salone.
•	Gestione degli orari: definizione degli orari di apertura, delle pause e dei giorni di chiusura, che alimentano automaticamente la logica di disponibilità degli slot.
•	Dashboard operativa: il barbiere dispone di una vista settimanale e di una vista giornaliera per monitorare l'agenda in ogni momento.
•	Flusso di prenotazione cliente: il cliente — registrato o ospite — può consultare la disponibilità, scegliere il servizio e la poltrona preferita, e inviare una richiesta che il barbiere accetta o rifiuta.
•	Notifiche: il barbiere riceve notifiche in-app per le nuove richieste; il cliente registrato riceve aggiornamenti in-app sullo stato della propria prenotazione; il cliente ospite viene notificato tramite canali esterni (SMS o contatto telefonico diretto).
1.3	Obiettivi e criteri di successo del sistema
Gli obiettivi principali della piattaforma BarberBook sono:
•	Eliminare la dipendenza dai fogli Excel condivisi, garantendo un'unica fonte di verità accessibile e aggiornata in tempo reale su tutti i dispositivi.
•	Ridurre il tempo dedicato alla gestione manuale delle prenotazioni, automatizzando la verifica della disponibilità e il ciclo di vita degli appuntamenti.
•	Migliorare l'esperienza del cliente, offrendo un canale self-service intuitivo per la consultazione delle disponibilità e l'invio delle richieste.
•	Garantire la scalabilità del sistema rispetto al numero di poltrone e al volume di prenotazioni gestibili.

I criteri di successo sono:
•	Funzionalità: il sistema deve implementare il 100% dei requisiti funzionali con priorità Alta.
•	Usabilità: il barbiere deve essere in grado di completare le operazioni più frequenti (visualizzazione agenda, accettazione prenotazione) in meno di 3 interazioni.
•	Accessibilità multi-dispositivo: il sistema deve essere pienamente operativo su PC, tablet e smartphone senza degradazione dell'esperienza.
•	Rispetto dei tempi: il progetto deve essere consegnato nei tempi definiti a priori.
1.4	Definizioni, acronimi e abbreviazioni
Acronimi:

RAD	Requirements Analysis Document — il presente documento
BAR	Barbiere — amministratore e unico gestore del sistema
CLR	Cliente Registrato — utente con account sulla piattaforma
CLG	Cliente Ospite (Guest) — utente non autenticato, senza account
CLI	Cliente Generico — comprende CLR e CLG quando il requisito è condiviso
RF	Requisito Funzionale
RNF	Requisito Non Funzionale
UC	Use Case — Caso d'Uso
SC	Scenario
UCD	Use Case Diagram — Diagramma dei Casi d'Uso
AD	Activity Diagram — Diagramma delle Attività
SD	Sequence Diagram — Diagramma di Sequenza
FURPS+	Modello per la classificazione dei requisiti non funzionali: Funzionalità, Usabilità, Affidabilità (Reliability), Prestazioni (Performance), Sostenibilità (Supportability), più pseudo-requisiti
CRUD	Create, Read, Update, Delete — operazioni standard di gestione dati
UI	User Interface — Interfaccia Utente
SMS	Short Message Service — canale di notifica esterno per clienti ospiti
1.5	Riferimenti
•	Object-Oriented Software Engineering (Using UML, Patterns, and Java), Third Edition — Bernd Bruegge & Allen H. Dutoit.
1.6	Organizzazione del documento
Il presente documento è strutturato come segue:
•	Sezione 1 – Introduzione: obiettivo, ambito, criteri di successo, glossario di acronimi e riferimenti.
•	Sezione 2 – Sistema Corrente: panoramica del processo attuale basato su fogli Excel e identificazione delle criticità.
•	Sezione 3 – Sistema Proposto: panoramica degli attori, requisiti funzionali (RF) e non funzionali (RNF), scenari d'uso, use cases, modello a oggetti e modello dinamico.
•	Sezione 4 – Glossario: definizione dei termini tecnici e di dominio utilizzati nel documento.
2	Sistema Corrente
2.1	Analisi del dominio
Il dominio di riferimento è la gestione operativa di un salone di barbiere con due poltrone. Il fulcro dell'attività è la prenotazione temporale: un cliente occupa una risorsa (poltrona + barbiere) per un intervallo di tempo variabile in base al servizio richiesto.
2.2	Flussi di lavoro attuali
Attualmente, il sistema si basa su fogli Excel condivisi (es. Google Sheets o Excel su OneDrive).
1.	Inserimento: Il barbiere apre il file dal dispositivo a disposizione e scrive manualmente il nome del cliente nella cella corrispondente all'orario e alla poltrona.
2.	Sincronizzazione: Il sistema si affida alla sincronizzazione cloud del fornitore (Microsoft/Google).
3.	Interazione Cliente: Non esiste un'interfaccia per il cliente. Le prenotazioni avvengono tramite canali esterni (telefono, WhatsApp, di persona). Il barbiere deve poi riportare manualmente questi dati sul foglio Excel.
2.3	Problemi e Criticità
L'analisi del sistema basato su Excel ha evidenziato i seguenti "pain points":
•	User Experience (UX) inefficiente: Inserire dati in celle piccole da uno smartphone è scomodo e soggetto a errori di battitura.
•	Conflitti di sincronizzazione: Se il file viene aperto simultaneamente su più dispositivi in zone con scarsa copertura, possono verificarsi perdite di dati o versioni del file non allineate.
•	Mancanza di automazione: Il barbiere deve verificare manualmente se uno slot è libero confrontando visivamente le colonne delle due poltrone.
•	Carico cognitivo: Il barbiere deve gestire contemporaneamente il lavoro in salone e le notifiche/chiamate dai clienti per le prenotazioni, dovendo poi "ricordarsi" di segnarle su Excel.
•	Assenza di Self-Service: Il cliente non può conoscere la disponibilità se non contattando direttamente il barbiere, aumentando le interruzioni durante l'attività lavorativa.
3	Sistema Proposto
3.1	Panoramica del sistema
BarberBook trasforma il processo di prenotazione da "inserimento manuale su griglia" a "gestione dinamica di flussi". Il sistema si interpone tra il cliente e l'agenda del barbiere, fungendo da filtro e orchestratore.
Il cuore del sistema è la logica di validazione: a differenza dei fogli Excel dove la scrittura equivale a conferma, qui esiste uno stato intermedio di "Richiesta". Il sistema garantisce che non ci siano sovrapposizioni (double-booking) grazie al controllo automatico degli slot basato sulla durata del servizio selezionato.
3.2	Requisiti Funzionali (RF)
3.2.1	Requisiti Generali (RF_GEN)
Funzionalità trasversali, condivise tra più attori del sistema.
ID	Nome Requisito	Descrizione (User Story)	Priorità
RF_GEN_1	Registrazione Account	Come CLR, voglio registrarmi alla piattaforma fornendo nome, cognome, e-mail/num. telefono e password, per accedere alle funzionalità riservate agli utenti registrati.	Alta
RF_GEN_2	Login	Come utente autenticabile (BAR o CLR), voglio effettuare il login con le mie credenziali per accedere in sicurezza alle funzionalità del sistema.	Alta
RF_GEN_3	Logout	Come utente autenticato (BAR o CLR), voglio effettuare il logout per terminare la sessione in modo sicuro.	Alta
RF_GEN_4	Recupero Password	Come CLR, voglio poter reimpostare la mia password tramite un link inviato alla mia e-mail, per riottenere l'accesso all'account in caso di smarrimento.	Media
RF_GEN_5	Notifiche In-App	Come utente autenticato (BAR o CLR), voglio ricevere notifiche in-app in tempo reale per essere informato su eventi rilevanti (nuove richieste, conferme, rifiuti, cancellazioni).	Alta
Nota: Il BAR non si registra e non può reimpostare la password in autonomia. Il suo account è creato e gestito esclusivamente dall'amministratore di sistema tramite accesso diretto al database. RF_GEN_1 e RF_GEN_4 sono pertanto esclusivi del CLR.
3.2.2	Lato Barbiere (RF_BAR)
Funzionalità esclusive per l'amministratore del sistema.
A. Gestione Operativa — Dashboard
ID	Nome Requisito	Descrizione (User Story)	Priorità
RF_BAR_1	Dashboard Settimanale	Come BAR, voglio visualizzare una vista d'insieme dell'intera settimana corrente, con tutti gli appuntamenti suddivisi per poltrona e per giorno, per pianificare il lavoro in anticipo.	Alta
RF_BAR_2	Dashboard Giornaliera	Come BAR, voglio visualizzare il dettaglio degli appuntamenti del giorno corrente in ordine cronologico per ciascuna poltrona, per gestire l'agenda in tempo reale.	Alta
B. Gestione Poltrone
ID	Nome Requisito	Descrizione (User Story)	Priorità
RF_BAR_3	Aggiunta Poltrona	Come BAR, voglio aggiungere una nuova poltrona al sistema assegnandole un nome, per riflettere l'espansione fisica del salone.	Media
RF_BAR_4	Rimozione Poltrona	Come BAR, voglio rimuovere una poltrona esistente dal sistema, per disattivarla in caso di indisponibilità permanente.	Media
RF_BAR_5	Rinomina Poltrona	Come BAR, voglio modificare il nome di una poltrona esistente, per personalizzarne l'identificativo in modo significativo.	Bassa
C. Gestione Servizi
ID	Nome Requisito	Descrizione (User Story)	Priorità
RF_BAR_6	Creazione Servizio	Come BAR, voglio creare un nuovo servizio nel catalogo specificando nome, durata stimata e prezzo, per renderlo disponibile alla selezione durante la prenotazione.	Alta
RF_BAR_7	Modifica Servizio	Come BAR, voglio modificare i dettagli di un servizio esistente (nome, durata, prezzo), per mantenere il catalogo aggiornato.	Alta
RF_BAR_8	Eliminazione Servizio	Come BAR, voglio eliminare un servizio dal catalogo, per rimuovere le offerte non più disponibili nel salone.	Media
D. Gestione Orari
ID	Nome Requisito	Descrizione (User Story)	Priorità
RF_BAR_9	Definizione Orari di Apertura	Come BAR, voglio definire gli orari di apertura e chiusura del salone per ciascun giorno della settimana, per alimentare automaticamente la logica di disponibilità degli slot.	Alta
RF_BAR_10	Gestione Pause	Come BAR, voglio inserire o rimuovere fasce orarie di pausa per una specifica poltrona in un dato giorno, per bloccare slot non disponibili.	Alta
E. Gestione Prenotazioni da Gestionale
ID	Nome Requisito	Descrizione (User Story)	Priorità
RF_BAR_11	Creazione Prenotazione Diretta	Come BAR, voglio creare manualmente una prenotazione direttamente dal gestionale specificando nominativo, poltrona, servizio, giorno e orario, per registrare appuntamenti presi telefonicamente o di persona.	Alta
RF_BAR_12	Modifica Prenotazione	Come BAR, voglio modificare una prenotazione esistente (orario, servizio, poltrona), per gestire spostamenti o variazioni richieste dal cliente.	Alta
RF_BAR_13	Cancellazione Prenotazione	Come BAR, voglio cancellare una prenotazione esistente, per liberare lo slot in caso di disdetta o imprevisto.	Alta
F. Gestione Richieste e Notifiche
ID	Nome Requisito	Descrizione (User Story)	Priorità
RF_BAR_14	Accettazione Richiesta	Come BAR, voglio accettare una richiesta di prenotazione ricevuta da un cliente, per confermarla e renderla visibile nell'agenda.	Alta
RF_BAR_15	Rifiuto Richiesta	Come BAR, voglio rifiutare una richiesta di prenotazione ricevuta da un cliente, per liberare lo slot e notificare il cliente dell'esito negativo.	Alta
RF_BAR_16	Area Notifiche	Come BAR, voglio visualizzare un'area notifiche dedicata con tutte le richieste in arrivo e gli aggiornamenti recenti, per gestirle in modo ordinato senza perdere nessuna comunicazione.	Alta
3.2.3	Lato Cliente - Utente Generico (RF_CLI)
Funzionalità comuni a utenti Registrati (CLR) e Ospiti (CLG).
ID	Nome    Requisito	Descrizione (User Story)	Priorità
RF_CLI_1	Vetrina Servizi	Come CLI, voglio visualizzare la lista completa dei servizi offerti dal salone con nome, durata e prezzo, per scegliere consapevolmente prima di prenotare.	Alta
RF_CLI_2	Visualizzazione Poltrone Disponibili	Come CLI, voglio visualizzare le poltrone attive del salone, per sapere su quali posso prenotare.	Alta
RF_CLI_3	Filtro per Giorno	Come CLI, voglio filtrare la disponibilità selezionando un giorno specifico, per vedere gli slot liberi nella data di mio interesse.	Alta
RF_CLI_4	Visualizzazione Slot Liberi	Come CLI, voglio visualizzare gli orari disponibili per ciascuna poltrona nel giorno selezionato, per scegliere la fascia oraria più comoda.	Alta
RF_CLI_5	Selezione Servizio in Prenotazione	Come CLI, voglio selezionare il servizio desiderato durante il flusso di prenotazione tra quelli disponibili nel catalogo, per comunicare al barbiere il tipo di prestazione richiesta.	Alta
RF_CLI_6	Invio Richiesta di Prenotazione	Come CLI, voglio inviare una richiesta di prenotazione per uno slot specifico, selezionando poltrona, servizio e orario, sapendo che sarà il barbiere a confermarla o rifiutarla.	Alta
3.2.4	Lato Cliente - Solo Registrato (RF_CLR)
Funzionalità avanzate per chi possiede un account sulla piattaforma.
ID	Nome Requisito	Descrizione (User Story)	Priorità
RF_CLR_1	Homepage Personale	Come CLR, voglio una homepage personalizzata con i miei prossimi appuntamenti confermati e le informazioni principali del salone, per avere tutto sotto controllo appena accedo.	Media
RF_CLR_2	Visualizzazione Storico Prenotazioni	Come CLR, voglio visualizzare l'elenco di tutte le mie prenotazioni (passate e future), per tenere traccia della mia storia nel salone.	Media
RF_CLR_3	Filtro Prenotazioni per Stato	Come CLR, voglio filtrare le mie prenotazioni per stato (in attesa, accettata, rifiutata, annullata, passata), per trovare rapidamente quelle di mio interesse.	Media
RF_CLR_4	Annullamento Prenotazione	Come CLR, voglio annullare una mia prenotazione futura inserendo una motivazione, per comunicare al barbiere la disdetta in modo strutturato.	Alta
RF_CLR_5	Riprenotazione Rapida	Come CLR, voglio riprenotare un servizio già usufruito in passato con un click che precompila poltrona e servizio, per velocizzare il processo per i clienti abituali.	Bassa
RF_CLR_6	Modifica Profilo Personale	Come CLR, voglio visualizzare e modificare i miei dati personali (nome, cognome, e-mail, numero di telefono), per mantenere il mio profilo aggiornato.	Alta
RF_CLR_7	Area Notifiche	Come CLR, voglio visualizzare un'area notifiche con gli aggiornamenti sulle mie prenotazioni (conferma, rifiuto, cancellazione da parte del barbiere), per essere sempre informato sull'esito delle mie richieste.	Alta

3.2.5	Lato Cliente - Solo Ospite (RF_CLG)
Vincoli e funzionalità specifiche per chi prenota senza account.
ID	Nome Requisito	Descrizione (User Story)	Priorità
RF_CLG_1	Form Dati Ospite	Come CLG, voglio inserire obbligatoriamente nome, cognome e numero di telefono durante il flusso di prenotazione, per permettere al barbiere di identificarmi e contattarmi per la conferma.	Alta
RF_CLG_2	Registrazione Veloce Post-Prenotazione	Come CLG, dopo aver inviato la mia richiesta voglio avere la possibilità di creare un account partendo dai dati già inseriti, per non doverli reinserire e accedere alle funzionalità avanzate.	Bassa
3.3	Requisiti Non Funzionali (RNF)
Classificati secondo il modello FURPS+.
3.3.1	Usabilità (Usability)
ID	Nome	Descrizione	Misura di Verifica	Priorità
RNF_U_1	Efficienza operativa BAR	Il barbiere deve poter completare le operazioni più frequenti (visualizzare agenda giornaliera, accettare/rifiutare una richiesta) senza superare 3 interazioni dalla schermata principale.	Test manuale: 3 utenti eseguono il task; il 100% lo completa in ≤ 3 click.	Alta
RNF_U_2	Responsive Design	L'interfaccia deve essere pienamente utilizzabile su PC, tablet e smartphone senza degradazione funzionale.	Verifica visiva e funzionale su almeno 3 viewport: 375px (mobile), 768px (tablet), 1280px (desktop).	Alta

3.3.2	Affidabilità (Reliability)
ID	Nome	Descrizione	Misura di Verifica	Priorità
RNF_R_1	Consistenza dei dati	Il sistema non deve mai consentire il double-booking: due prenotazioni non possono occupare lo stesso slot sulla stessa poltrona.	Test automatico: 100 tentativi di prenotazione concorrente sullo stesso slot; il sistema ne accetta esattamente 1 e rifiuta i restanti.	Alta

3.3.3	Prestazioni (Performance)
ID	Nome	Descrizione	Misura di Verifica	Priorità
RNF_P_1	Tempo di risposta	Le pagine principali del sistema (dashboard, lista slot disponibili) devono caricarsi in tempi accettabili in condizioni di rete standard.	Misurazione con browser DevTools: tempo di caricamento ≤ 3 secondi su connessione 4G simulata (10 Mbps).	Media

3.3.4	Supportabilità (Supportability)
ID	Nome	Descrizione	Misura di Verifica	Priorità
RNF_S_1	Compatibilità browser	Il sistema deve funzionare correttamente sui browser più diffusi senza richiedere configurazioni aggiuntive.	Verifica funzionale su Chrome, Safari e Firefox nelle rispettive versioni stabili più recenti al momento del rilascio.	Media

3.3.5	Pseudo-requisiti (+)
ID	Nome	Descrizione	Priorità
RNF_X_1	Lingua	L'intera interfaccia utente deve essere in lingua italiana.	Alta
RNF_X_2	Sicurezza accessi	Le aree riservate devono essere protette da autenticazione. Un utente non autenticato non può accedere a funzionalità riservate a BAR o CLR.	Alta
3.4	Scenari
Gli scenari seguenti sono di tipo Visionary: descrivono il sistema futuro dal punto di vista concreto di un singolo attore in una situazione specifica. Il linguaggio adottato è quello del dominio applicativo, privo di riferimenti tecnici.
3.4.1	Scenario 1 — Gestione dell'agenda del giorno
Nome scenario: Consultazione e gestione dell'agenda giornaliera
Attori partecipanti: Tony (BAR)
Flusso degli eventi:
È lunedì mattina. Tony apre BarberBook dal suo smartphone prima ancora di entrare in salone. Accede con le sue credenziali e atterra direttamente sulla dashboard giornaliera, dove vede in colpo d'occhio tutti gli appuntamenti della giornata suddivisi per poltrona. Nota che alle 11:00 sulla Poltrona 1 c'è un buco inaspettato: un cliente ha annullato la sera prima. Tony decide di colmare lo slot creando direttamente una prenotazione manuale per un cliente abituale che gli aveva mandato un messaggio su WhatsApp. Inserisce il nominativo, seleziona il servizio "Capelli + Barba" e conferma l'orario. La prenotazione appare immediatamente nell'agenda. Tony chiude l'app e inizia la giornata con la situazione completamente sotto controllo.
3.4.2	Scenario 2 — Ricezione e gestione di una richiesta cliente
Nome scenario: Accettazione di una richiesta di prenotazione
Attori partecipanti: Tony (BAR), Marco (CLR)
Flusso degli eventi:
Marco è un cliente abituale del salone di Tony. Nel pomeriggio di mercoledì, durante la pausa pranzo, apre BarberBook dal suo telefono. Naviga nella sezione di prenotazione, seleziona giovedì come giorno, sceglie il servizio "Barba" e visualizza gli slot liberi sulle due poltrone. Sceglie la Poltrona 2 alle 17:30 e invia la richiesta. Il sistema gli mostra un messaggio che lo informa che la prenotazione è in attesa di conferma da parte del barbiere.
Tony riceve una notifica in-app. Apre l'area notifiche e vede la richiesta di Marco con tutti i dettagli: nome, servizio, poltrona e orario. Tony controlla che lo slot sia effettivamente libero nella sua agenda e accetta la richiesta con un singolo tap. Marco riceve immediatamente una notifica in-app che lo informa della conferma. L'appuntamento compare ora nell'agenda di Tony e nello storico prenotazioni di Marco con stato "Accettata".
3.4.3	Scenario 3 — Prenotazione da parte di un cliente ospite
Nome scenario: Invio richiesta di prenotazione senza account
Attori partecipanti: Luigi (CLG)
Flusso degli eventi:
Luigi passa davanti al salone di Tony e nota un cartello con il QR code di BarberBook. Lo scansiona dal telefono e accede alla piattaforma senza registrarsi. Sfoglia la vetrina dei servizi e sceglie "Capelli Junior" per suo figlio. Seleziona sabato come giorno e visualizza gli orari disponibili. Sceglie la Poltrona 1 alle 10:00. Poiché non ha un account, il sistema gli chiede di inserire nome, cognome e numero di telefono prima di procedere. Luigi compila il form e invia la richiesta. Il sistema lo informa che la richiesta è stata ricevuta e che Tony lo contatterà telefonicamente per la conferma. Al termine, il sistema gli propone di creare un account con i dati appena inseriti per semplificare le prenotazioni future: Luigi per il momento rifiuta e chiude l'app.
3.4.4	Scenario 4 — Annullamento di una prenotazione da parte del cliente
Nome scenario: Cancellazione prenotazione con motivazione
Attori partecipanti: Marco (CLR), Tony (BAR)
Flusso degli eventi:
Marco ha un appuntamento confermato per venerdì alle 18:00. Il giovedì sera si accorge di avere un imprevisto lavorativo e non potrà presentarsi. Apre BarberBook, va nella sezione "Le mie prenotazioni" e trova l'appuntamento di venerdì con stato "Accettata". Seleziona l'opzione di annullamento, il sistema gli chiede di inserire una motivazione: Marco scrive "Imprevisto lavorativo, mi scuso per il disagio". Conferma l'annullamento. Tony riceve immediatamente una notifica in-app con il dettaglio della cancellazione e la motivazione inserita da Marco. Lo slot delle 18:00 di venerdì torna automaticamente disponibile nell'agenda.
3.4.5	Scenario 5 — Configurazione iniziale del salone
Nome scenario: Prima configurazione di servizi e orari
Attori partecipanti: Tony (BAR)
Flusso degli eventi:
È il primo giorno di utilizzo di BarberBook. Tony accede al gestionale dal PC di casa e si dedica alla configurazione del salone. Per prima cosa accede alla sezione servizi e crea le voci del suo catalogo: inserisce "Capelli" con durata 30 minuti e prezzo 15€, poi "Barba" con durata 20 minuti e 10€, "Capelli + Barba" con 45 minuti e 22€, e così via fino a completare l'offerta. Successivamente accede alla sezione poltrone: le due poltrone esistenti le rinomina "Poltrona Mario" e "Poltrona Luca" in onore dei suoi due collaboratori. Infine, imposta gli orari di apertura: dal lunedì al sabato, dalle 9:00 alle 19:00, con una pausa pranzo dalle 13:00 alle 15:00 valida per entrambe le poltrone. Una volta terminata la configurazione, Tony apre la dashboard settimanale e vede la griglia degli slot già popolata e pronta a ricevere prenotazioni. Soddisfatto, condivide il link della piattaforma sul suo profilo WhatsApp.

3.5	Use Cases
I casi d'uso seguenti formalizzano i flussi critici già narrati negli scenari. Per ciascuno viene descritto il flusso principale e, dove significativo, i flussi alternativi e di eccezione. La UI viene deliberatamente ignorata: si descrive la logica dell'interazione, non il layout.

3.5.1	UC_BAR_1 — GestioneAgendaGiornaliera
Attori partecipanti: Tony: BAR (iniziatore)
Condizioni di ingresso:
•	Il BAR è autenticato nel sistema.
•	Esiste almeno una poltrona configurata nel salone.
Flusso degli eventi:
Passo	Attore	Sistema
1	Il BAR accede alla sezione Dashboard Giornaliera.	
2		Il sistema recupera tutte le prenotazioni del giorno corrente e le presenta in ordine cronologico, raggruppate per poltrona.
3	Il BAR consulta l'agenda e individua uno slot libero.	
4	Il BAR avvia la creazione di una nuova prenotazione manuale, specificando nominativo, servizio, poltrona e orario.	
5		Il sistema verifica che lo slot selezionato sia effettivamente libero per la poltrona indicata.
6		Il sistema crea la prenotazione con stato "Confermata" e la inserisce nell'agenda.
7		Il sistema aggiorna la vista della dashboard mostrando il nuovo appuntamento.
Flusso alternativo A — Lo slot è già occupato (passo 5):
•	A.1 Il sistema rileva una sovrapposizione con una prenotazione esistente.
•	A.2 Il sistema notifica il BAR che lo slot non è disponibile.
•	A.3 Il BAR seleziona un orario alternativo. Il flusso riprende dal passo 5.
Flusso alternativo B — Il BAR modifica una prenotazione esistente:
•	B.1 Al passo 3, il BAR seleziona una prenotazione esistente anziché crearne una nuova.
•	B.2 Il BAR modifica uno o più campi (orario, servizio, poltrona).
•	B.3 Il sistema verifica la disponibilità del nuovo slot e aggiorna la prenotazione. Il flusso riprende dal passo 7.
Flusso alternativo C — Il BAR cancella una prenotazione esistente:
•	C.1 Al passo 3, il BAR seleziona una prenotazione esistente e avvia la cancellazione.
•	C.2 Il sistema rimuove la prenotazione dall'agenda e libera lo slot.
•	C.3 Se la prenotazione era stata originata da una richiesta CLR, il sistema invia una notifica in-app al cliente.
Condizioni di uscita:
•	L'agenda giornaliera è aggiornata e riflette correttamente le modifiche apportate dal BAR.
•	Nessuno slot risulta prenotato due volte sulla stessa poltrona.
Requisiti di qualità:
•	RNF_R_1 (no double-booking).
•	RNF_P_1 (caricamento dashboard ≤ 3 secondi).

3.5.2	UC_BAR_2 — GestioneRichiestaDiPrenotazione
Attori partecipanti: Tony: BAR (iniziatore), Marco: CLR (partecipante notificato)
Condizioni di ingresso:
•	Il BAR è autenticato nel sistema.
•	Esiste almeno una richiesta di prenotazione con stato "In Attesa" nell'area notifiche.
Flusso degli eventi:
Passo	Attore	Sistema
1	Il BAR accede all'area notifiche.	
2		Il sistema presenta l'elenco delle richieste in attesa, ciascuna con: nome cliente (o dati ospite), servizio, poltrona, giorno e orario richiesti.
3	Il BAR seleziona una richiesta e la esamina.	
4	Il BAR accetta la richiesta.	
5		Il sistema verifica che lo slot sia ancora libero per la poltrona indicata.
6		Il sistema aggiorna lo stato della prenotazione da "In Attesa" a "Confermata" e la inserisce nell'agenda.
7		Il sistema invia una notifica in-app al CLR informandolo della conferma.
Flusso alternativo A — Il BAR rifiuta la richiesta (passo 4):
•	A.1 Il BAR seleziona l'opzione di rifiuto.
•	A.2 Il sistema aggiorna lo stato della richiesta a "Rifiutata".
•	A.3 Il sistema invia una notifica in-app al CLR informandolo del rifiuto.
•	A.4 Lo slot rimane libero nell'agenda.
Flusso alternativo B — Lo slot non è più disponibile al momento dell'accettazione (passo 5):
•	B.1 Il sistema rileva che lo slot è stato nel frattempo occupato da un'altra prenotazione.
•	B.2 Il sistema notifica il BAR del conflitto e non conferma la richiesta.
•	B.3 Il BAR può rifiutare la richiesta o contattare il cliente per concordare un orario alternativo. Il flusso riprende dal passo 4.
Flusso di eccezione — La richiesta proviene da un CLG:
•	E.1 Il sistema mostra i dati anagrafici inseriti dal CLG (nome, cognome, telefono) al posto del profilo registrato.
•	E.2 In caso di accettazione, nessuna notifica in-app viene inviata: il BAR provvede a contattare il CLG tramite il numero di telefono indicato.
•	E.3 Il flusso prosegue normalmente dal passo 6.
Condizioni di uscita:
•	La richiesta ha cambiato stato ("Confermata" o "Rifiutata").
•	Se confermata, la prenotazione è visibile nell'agenda del BAR e nello storico del CLR.
•	Se rifiutata, lo slot è libero e il CLR è stato notificato.
Requisiti di qualità:
•	RNF_R_1 (no double-booking).
•	RNF_U_1 (accettazione/rifiuto completabili in ≤ 3 interazioni dalla schermata principale).

3.5.3	UC_CLI_1 — InvioRichiestaPrenotazione
Attori partecipanti: Luigi: CLG (iniziatore) — il flusso è identico per CLR, con le differenze indicate nel flusso alternativo A
Condizioni di ingresso:
•	Il sistema ha almeno un servizio configurato nel catalogo.
•	Il sistema ha almeno una poltrona attiva con slot liberi nella settimana corrente o futura.
Flusso degli eventi:
Passo	Attore	Sistema
1	Il CLI accede alla sezione di prenotazione.	
2		Il sistema presenta la vetrina dei servizi disponibili con nome, durata e prezzo.
3	Il CLI seleziona il servizio desiderato.	
4	Il CLI seleziona il giorno preferito.	
5		Il sistema calcola e presenta gli slot liberi per ciascuna poltrona attiva nel giorno selezionato, tenendo conto della durata del servizio scelto e degli orari di apertura.
6	Il CLI seleziona la poltrona e lo slot orario desiderati.	
7		Poiché il CLI non è autenticato, il sistema richiede l'inserimento di nome, cognome e numero di telefono.
8	Il CLG compila il form con i propri dati anagrafici.	
9	Il CLI conferma la richiesta.	
10		Il sistema crea la prenotazione con stato "In Attesa" e invia una notifica al BAR.
11		Il sistema informa il CLG che la richiesta è stata ricevuta e che sarà contattato telefonicamente per la conferma.
12		Il sistema propone al CLG di creare un account con i dati appena inseriti.
Flusso alternativo A — Il CLI è un CLR autenticato (passo 7):
•	A.1 Il sistema non richiede l'inserimento dei dati anagrafici poiché il CLR è già identificato.
•	A.2 Il flusso salta i passi 7 e 8 e prosegue direttamente al passo 9.
•	A.3 Dopo la conferma, il sistema informa il CLR che sarà notificato in-app dell'esito.
Flusso alternativo B — Nessuno slot disponibile nel giorno selezionato (passo 5):
•	B.1 Il sistema informa il CLI che non vi sono slot disponibili per il giorno scelto con il servizio selezionato.
•	B.2 Il CLI seleziona un giorno alternativo. Il flusso riprende dal passo 5.
Flusso di eccezione — Il CLI non completa il form dei dati ospite (passo 8):
•	E.1 Il CLI lascia uno o più campi obbligatori vuoti e tenta di procedere.
•	E.2 Il sistema segnala i campi mancanti e blocca l'invio della richiesta fino alla loro compilazione.
Condizioni di uscita:
•	Una prenotazione con stato "In Attesa" è registrata nel sistema.
•	Il BAR ha ricevuto una notifica della nuova richiesta.
•	Il CLI è stato informato dell'avvenuta ricezione della richiesta.
Requisiti di qualità:
•	RNF_R_1 (no double-booking: il sistema non presenta slot già occupati).
•	RNF_U_2 (il flusso deve essere completabile da smartphone senza degradazione).
3.5.4	UC_CLR_1 — GestioneProfiloEStorico
Attori partecipanti: Marco: CLR (iniziatore)
Condizioni di ingresso:
•	Il CLR è autenticato nel sistema.
Flusso degli eventi:
Passo	Attore	Sistema
1	Il CLR accede alla sezione profilo personale.	
2		Il sistema presenta i dati personali attuali del CLR (nome, cognome, e-mail, numero di telefono).
3	Il CLR modifica uno o più campi e conferma.	
4		Il sistema valida i dati inseriti e salva le modifiche.
5	Il CLR accede alla sezione storico prenotazioni.	
6		Il sistema presenta l'elenco completo delle prenotazioni del CLR in ordine cronologico decrescente.
7	Il CLR applica un filtro per stato (es. "In Attesa", "Confermata", "Rifiutata", "Annullata", "Passata").	
8		Il sistema aggiorna la lista mostrando solo le prenotazioni corrispondenti al filtro selezionato.
Flusso alternativo A — Il CLR annulla una prenotazione futura (passo 6):
•	A.1 Il CLR seleziona una prenotazione con stato "Confermata" o "In Attesa".
•	A.2 Il CLR avvia l'annullamento e inserisce una motivazione testuale.
•	A.3 Il sistema aggiorna lo stato della prenotazione a "Annullata" e libera lo slot nell'agenda.
•	A.4 Il sistema invia una notifica al BAR con i dettagli della cancellazione e la motivazione inserita.
Flusso alternativo B — Il CLR effettua una riprenotazione rapida (passo 6):
•	B.1 Il CLR seleziona una prenotazione passata e avvia la riprenotazione rapida.
•	B.2 Il sistema pre-compila il flusso di prenotazione con il servizio e la poltrona della prenotazione precedente.
•	B.3 Il flusso prosegue dal passo 4 di UC_CLI_1, con il CLR che seleziona solo giorno e orario.
Flusso di eccezione — Dati non validi in modifica profilo (passo 4):
•	E.1 Il sistema rileva un formato non valido (es. e-mail malformata, telefono con caratteri non numerici).
•	E.2 Il sistema segnala il campo errato e non salva le modifiche fino alla correzione.
Condizioni di uscita:
•	I dati del profilo sono aggiornati nel sistema.
•	Lo storico prenotazioni riflette l'eventuale annullamento effettuato.
•	Il BAR è stato notificato di eventuali cancellazioni.
Requisiti di qualità:
•	RNF_X_2 (solo il CLR autenticato può accedere al proprio profilo e storico).
3.6	Modello ad Oggetti
Il modello ad oggetti identifica le classi del sistema a partire dall'analisi dei casi d'uso e degli scenari. Seguendo la metodologia di Bruegge & Dutoit, le classi sono suddivise in tre categorie: Entity, Boundary e Control. L'identificazione avviene esaminando i sostantivi ricorrenti nei casi d'uso (candidati Entity e Boundary) e i verbi che descrivono processi decisionali o di coordinamento (candidati Control).
3.6.1	Entity Objects
Le classi Entity rappresentano le informazioni persistenti del sistema, indipendenti dall'interfaccia e dalla logica applicativa. Sopravvivono al termine delle sessioni utente.
•	Utente Classe astratta che rappresenta qualsiasi soggetto autenticato nel sistema. Contiene gli attributi comuni a BAR e CLR: identificativo univoco, nome, cognome, numero di telefono, e-mail e ruolo. Non viene istanziata direttamente.
•	Barbiere (specializza Utente) Rappresenta l'account del BAR. Non si registra autonomamente: il suo account è creato dall'amministratore di sistema. Ha accesso esclusivo alle funzionalità di gestione del salone.
•	ClienteRegistrato (specializza Utente) Rappresenta un cliente con account sulla piattaforma. Possiede in aggiunta e-mail verificata e password. Può visualizzare lo storico delle proprie prenotazioni e ricevere notifiche in-app.
•	ClienteOspite Rappresenta un cliente non autenticato che richiede una prenotazione senza creare un account. Non è una specializzazione di Utente poiché non possiede credenziali. Attributi: nome, cognome, numero di telefono. Ha ciclo di vita limitato alla singola richiesta di prenotazione, con possibilità di conversione in ClienteRegistrato al termine del flusso.
•	Poltrona Rappresenta una risorsa fisica del salone. Attributi: identificativo, nome personalizzato, stato (attiva/inattiva). Una poltrona può avere associate più prenotazioni e più fasce orarie di pausa.
•	Servizio Rappresenta una voce del catalogo offerto dal salone. Attributi: identificativo, nome, durata stimata in minuti, prezzo. Un servizio può essere associato a più prenotazioni. Viene creato, modificato ed eliminato esclusivamente dal BAR.
•	Prenotazione Classe centrale del sistema. Rappresenta un appuntamento nel salone, sia che provenga da una richiesta cliente sia che sia stata inserita manualmente dal BAR. Attributi: identificativo, data, orario di inizio, orario di fine (calcolato dalla durata del servizio), stato, motivazione di annullamento (opzionale). Associazioni: riferimento alla Poltrona, al Servizio, al Barbiere assegnato e al soggetto prenotante (ClienteRegistrato o ClienteOspite). Lo stato può assumere i valori: In Attesa, Confermata, Rifiutata, Annullata, Passata.
•	FasciaOraria Rappresenta un intervallo temporale configurato dal BAR per definire la disponibilità del salone. Attributi: giorno della settimana, ora di inizio, ora di fine, tipo (apertura o pausa), riferimento alla poltrona di appartenenza. È la base su cui il sistema calcola gli slot disponibili.
•	Notifica Rappresenta un messaggio generato dal sistema a seguito di un evento rilevante (nuova richiesta, conferma, rifiuto, cancellazione). Attributi: identificativo, testo, timestamp di creazione, stato (letta/non letta), riferimento al destinatario (BAR o CLR). Non applicabile al CLG, che viene notificato tramite canali esterni.

3.6.2	Boundary Objects
Le classi Boundary rappresentano le interfacce tra gli attori e il sistema. Ogni schermata o punto di interazione rilevante viene modellato come Boundary. Non contengono logica applicativa.
•	LoginUI Interfaccia di accesso al sistema, condivisa da BAR e CLR. Raccoglie le credenziali e le invia al sistema per la verifica. Presenta anche il link per il recupero password (solo CLR).
•	RegistrazioneUI Interfaccia dedicata alla creazione di un nuovo account CLR. Raccoglie nome, cognome, e-mail, password e numero di telefono.
•	DashboardSettimanaleUI (BAR) Interfaccia principale del BAR. Presenta la griglia settimanale degli appuntamenti suddivisi per poltrona e per giorno. Punto di accesso alle operazioni di gestione prenotazione diretta.
•	DashboardGiornalieraUI (BAR) Interfaccia di dettaglio giornaliero per il BAR. Presenta gli appuntamenti del giorno corrente in ordine cronologico per poltrona. Consente di creare, modificare e cancellare prenotazioni.
•	GestioneServizioUI (BAR) Interfaccia per il CRUD del catalogo servizi. Permette al BAR di creare, modificare ed eliminare le voci del catalogo.
•	GestionePoltronaUI (BAR) Interfaccia per la gestione delle poltrone fisiche del salone. Permette al BAR di aggiungere, rinominare e rimuovere poltrone.
•	GestioneOrariUI (BAR) Interfaccia per la configurazione degli orari di apertura e delle pause, per giorno della settimana e per poltrona.
•	AreaNotificheUI (BAR e CLR) Interfaccia di ricezione notifiche. Per il BAR mostra le nuove richieste in arrivo e gli aggiornamenti sulle prenotazioni. Per il CLR mostra le conferme, i rifiuti e le cancellazioni relative ai propri appuntamenti.
•	VetrinaServiziUI (CLI) Interfaccia pubblica che presenta il catalogo dei servizi del salone con nome, durata e prezzo. Accessibile senza autenticazione. Punto di ingresso al flusso di prenotazione.
•	PrenotazioneUI (CLI) Interfaccia del flusso di prenotazione cliente. Guida il CLI nella selezione del servizio, del giorno, della poltrona e dello slot orario. Si biforca in base allo stato di autenticazione: se CLR presenta direttamente il riepilogo, se CLG richiede la compilazione del form dati ospite.
•	FormDatiOspiteUI (CLG) Sotto-interfaccia del flusso di prenotazione, attivata esclusivamente per i clienti non autenticati. Raccoglie nome, cognome e numero di telefono obbligatori. Al termine propone la registrazione veloce.
•	HomepagePersonaleUI (CLR) Interfaccia principale del cliente registrato. Mostra i prossimi appuntamenti confermati e le informazioni del salone.
•	StoricoPrenotazioniUI (CLR) Interfaccia per la visualizzazione e il filtraggio delle prenotazioni del CLR. Consente di applicare filtri per stato e di avviare le operazioni di annullamento e riprenotazione rapida.
•	ProfiloPersonaleUI (CLR) Interfaccia per la visualizzazione e modifica dei dati personali del CLR.
3.6.3	Control Objects
Le classi Control orchestrano i flussi applicativi, coordinano le interazioni tra Boundary e Entity e contengono la logica di business del sistema. Non gestiscono persistenza né presentazione.
•	AuthController Gestisce i flussi di autenticazione e registrazione. Responsabilità: verifica delle credenziali in fase di login, creazione del nuovo account CLR, gestione della sessione attiva, avvio del flusso di recupero password. Coordina LoginUI, RegistrazioneUI, Utente.
•	PrenotazioneController Controller centrale del sistema. Gestisce l'intero ciclo di vita di una prenotazione. Responsabilità: calcolo degli slot disponibili a partire dalle FasceOrarie e dalla durata del Servizio selezionato, verifica dell'assenza di sovrapposizioni (anti double-booking), creazione della prenotazione con stato iniziale corretto ("Confermata" se inserita dal BAR, "In Attesa" se proveniente da richiesta cliente), aggiornamento dello stato a seguito di accettazione, rifiuto o cancellazione. Coordina PrenotazioneUI, DashboardGiornalieraUI, Prenotazione, Poltrona, Servizio, NotificaController.
•	NotificaController Gestisce la generazione e la consegna delle notifiche in-app. Responsabilità: creazione di una Notifica a seguito di eventi rilevanti (nuova richiesta, conferma, rifiuto, cancellazione), instradamento al destinatario corretto (BAR o CLR), aggiornamento dello stato di lettura. Coordina AreaNotificheUI e Notifica. Non gestisce le notifiche verso CLG, che avvengono fuori dal sistema.
•	ConfigurazioneSaloneController Gestisce le operazioni di configurazione del salone da parte del BAR. Responsabilità: CRUD sui Servizi, CRUD sulle Poltrone, gestione delle FasceOrarie di apertura e pausa. Coordina GestioneServizioUI, GestionePoltronaUI, GestioneOrariUI con le rispettive classi Entity.
•	ProfiloController Gestisce le operazioni sul profilo del CLR. Responsabilità: lettura e aggiornamento dei dati personali, validazione dei formati (email, telefono), recupero dello storico prenotazioni con applicazione dei filtri per stato. Coordina ProfiloPersonaleUI, StoricoPrenotazioniUI, ClienteRegistrato, Prenotazione.

3.6.4	Riepilogo delle classi
Categoria	Classi
Entity	Utente, Barbiere, ClienteRegistrato, ClienteOspite, Poltrona, Servizio, Prenotazione, FasciaOraria, Notifica
Boundary	LoginUI, RegistrazioneUI, DashboardSettimanaleUI, DashboardGiornalieraUI, GestioneServizioUI, GestionePoltronaUI, GestioneOrariUI, AreaNotificheUI, VetrinaServiziUI, PrenotazioneUI, FormDatiOspiteUI, HomepagePersonaleUI, StoricoPrenotazioniUI, ProfiloPersonaleUI
Control	AuthController, PrenotazioneController, NotificaController, ConfigurazioneSaloneController, ProfiloController
3.7	Modello Dinamico
Il modello dinamico descrive il comportamento del sistema nel tempo, mostrando come gli oggetti collaborano e si scambiano messaggi per realizzare i flussi critici. In coerenza con le scelte progettuali del documento, vengono presentati due Sequence Diagram — descritti in forma testuale — relativi ai flussi di maggiore complessità e rilevanza architetturale: il ciclo di vita completo di una prenotazione richiesta da un cliente e la gestione diretta dell'agenda da parte del BAR.

3.7.1	SD_1 — Invio e gestione di una richiesta di prenotazione (CLR → BAR)
Questo sequence diagram descrive il flusso più critico del sistema: una richiesta nasce dal cliente, attraversa il controller centrale, attende la decisione del barbiere e si risolve in una notifica di esito. È il flusso che distingue architetturalmente BarberBook da un semplice calendario condiviso.
Oggetti coinvolti: Marco: CLR — PrenotazioneUI — PrenotazioneController — Prenotazione — NotificaController — AreaNotificheUI (BAR) — Tony : BAR — AreaNotificheUI (CLR)
Sequenza dei messaggi:
1.	Marco → PrenotazioneUI — Il CLR accede al flusso di prenotazione e seleziona servizio, giorno e poltrona desiderati.
2.	PrenotazioneUI → PrenotazioneController — La UI invia la richiesta di calcolo disponibilità con i parametri selezionati (servizio, giorno, poltrona).
3.	PrenotazioneController → FasciaOraria — Il controller interroga le fasce orarie configurate per la poltrona e il giorno selezionati.
4.	PrenotazioneController → Prenotazione — Il controller interroga le prenotazioni esistenti per verificare l'assenza di sovrapposizioni, tenendo conto della durata del servizio.
5.	PrenotazioneController → PrenotazioneUI — Il controller restituisce la lista degli slot liberi disponibili.
6.	Marco → PrenotazioneUI — Il CLR seleziona lo slot desiderato e conferma la richiesta.
7.	PrenotazioneUI → PrenotazioneController — La UI trasmette la richiesta di prenotazione completa (CLR, servizio, poltrona, slot).
8.	PrenotazioneController → Prenotazione — Il controller crea una nuova istanza di Prenotazione con stato In Attesa.
9.	PrenotazioneController → NotificaController — Il controller delega la generazione della notifica per il BAR.
10.	NotificaController → Notifica — Viene creata una nuova istanza di Notifica destinata al BAR con i dettagli della richiesta.
11.	NotificaController → AreaNotificheUI (BAR) — La notifica viene recapitata all'area notifiche del BAR.
12.	PrenotazioneController → PrenotazioneUI — Il controller conferma al CLR che la richiesta è stata inviata con successo e che è in attesa di approvazione.
— Il sistema attende la decisione del BAR —
13.	Tony → AreaNotificheUI (BAR) — Il BAR apre l'area notifiche e visualizza la richiesta di Marco.
14.	Tony → AreaNotificheUI (BAR) — Il BAR accetta la richiesta.
15.	AreaNotificheUI (BAR) → PrenotazioneController — La UI trasmette la decisione di accettazione con riferimento alla prenotazione.
16.	PrenotazioneController → Prenotazione — Il controller aggiorna lo stato della prenotazione da In Attesa a Confermata.
17.	PrenotazioneController → NotificaController — Il controller delega la generazione della notifica di esito per il CLR.
18.	NotificaController → Notifica — Viene creata una nuova istanza di Notifica destinata al CLR con l'esito positivo.
19.	NotificaController → AreaNotificheUI (CLR) — La notifica viene recapitata all'area notifiche di Marco.
20.	Marco → AreaNotificheUI (CLR) — Il CLR visualizza la conferma della propria prenotazione.
Flusso alternativo — Il BAR rifiuta la richiesta (passo 14):
Il BAR seleziona il rifiuto anziché l'accettazione.
Al passo 16 lo stato viene aggiornato a Rifiutata e lo slot rimane libero.
I passi 17-20 si svolgono identicamente ma con notifica di esito negativo.
 

3.7.2	SD_2 — Creazione diretta di una prenotazione da gestionale (BAR)
Questo sequence diagram descrive il flusso esclusivo del BAR: la creazione manuale di un appuntamento senza passare dal flusso cliente. È architetturalmente rilevante perché bypassa lo stato "In Attesa" e genera una prenotazione immediatamente confermata, replicando il comportamento del vecchio sistema Excel ma con controllo automatico delle sovrapposizioni.
Oggetti coinvolti: Tony: BAR — DashboardGiornalieraUI — PrenotazioneController — FasciaOraria — Prenotazione — NotificaController
Sequenza dei messaggi:
1.	Tony → DashboardGiornalieraUI — Il BAR accede alla dashboard giornaliera e individua uno slot libero.
2.	Tony → DashboardGiornalieraUI — Il BAR avvia la creazione di una nuova prenotazione manuale specificando nominativo, servizio, poltrona, giorno e orario.
3.	DashboardGiornalieraUI → PrenotazioneController — La UI trasmette al controller i dati della nuova prenotazione.
4.	PrenotazioneController → FasciaOraria — Il controller verifica che l'orario indicato rientri nelle fasce di apertura configurate per quella poltrona.
5.	PrenotazioneController → Prenotazione — Il controller verifica l'assenza di sovrapposizioni con prenotazioni esistenti nello stesso slot e sulla stessa poltrona.
6.	(slot disponibile) PrenotazioneController → Prenotazione — Il controller crea una nuova istanza di Prenotazione con stato direttamente Confermata, senza passare per In Attesa.
7.	PrenotazioneController → DashboardGiornalieraUI — Il controller notifica l'avvenuta creazione e la UI aggiorna la vista dell'agenda mostrando il nuovo appuntamento.
Flusso alternativo — Lo slot è occupato (passo 5):
•	Il controller rileva una sovrapposizione con una prenotazione esistente.
•	Il controller restituisce un errore di conflitto alla DashboardGiornalieraUI.
•	La UI informa il BAR che lo slot non è disponibile e lo invita a selezionare un orario alternativo.
•	Il flusso riprende dal passo 2.
 
4	Glossario
Il glossario raccoglie i termini del dominio applicativo e i concetti tecnici utilizzati nel documento. L'obiettivo è garantire un vocabolario condiviso e non ambiguo tra tutti i lettori del RAD, indipendentemente dal loro background tecnico.
•	Agenda Rappresentazione visiva degli appuntamenti del salone organizzati per giorno e per poltrona. Nel sistema è realizzata attraverso la Dashboard Settimanale e la Dashboard Giornaliera.
•	Annullamento Operazione con cui un CLR disdice una propria prenotazione confermata o in attesa, fornendo obbligatoriamente una motivazione testuale. A differenza della cancellazione operata dal BAR, l'annullamento è sempre accompagnato da una motivazione e genera una notifica verso il BAR.
•	Appuntamento Sinonimo informale di Prenotazione confermata. Nel contesto del documento i due termini sono intercambiabili quando lo stato della prenotazione è Confermata.
•	Area Notifiche Sezione dedicata del sistema accessibile sia al BAR che al CLR. Per il BAR raccoglie le nuove richieste in arrivo e gli aggiornamenti sull'agenda. Per il CLR raccoglie gli esiti delle proprie richieste di prenotazione (conferma, rifiuto, cancellazione da parte del barbiere).
•	Attore Entità esterna al sistema che interagisce con esso per raggiungere un obiettivo. Nel presente documento gli attori sono: BAR, CLR, CLG. Vedere anche le definizioni individuali di ciascun attore.
•	Autenticazione Processo mediante il quale il sistema verifica l'identità di un utente tramite le proprie credenziali (email e password). Solo BAR e CLR sono soggetti ad autenticazione. Il CLG non si autentica.
•	BAR (Barbiere) Attore amministratore del sistema. Gestisce l'intera configurazione del salone (poltrone, servizi, orari) e ha pieno controllo sul ciclo di vita delle prenotazioni. Il suo account non viene creato tramite la piattaforma ma è pre-configurato dall'amministratore di sistema esterno.
•	Cancellazione Operazione con cui il BAR elimina direttamente una prenotazione dall'agenda, liberando lo slot corrispondente. Se la prenotazione era stata originata da un CLR, il sistema genera automaticamente una notifica verso il cliente.
•	Catalogo Servizi Insieme dei servizi offerti dal salone, configurato e mantenuto dal BAR. Ogni servizio è caratterizzato da nome, durata stimata e prezzo. Il catalogo è visibile a tutti i clienti nella Vetrina Servizi.
•	CLG (Cliente Ospite / Guest) Attore che interagisce con il sistema senza possedere un account. Può consultare la disponibilità e inviare richieste di prenotazione fornendo nome, cognome e numero di telefono. Non riceve notifiche in-app: viene contattato dal BAR tramite canali esterni (telefono).
•	CLI (Cliente Generico) Termine aggregato che indica indistintamente CLR e CLG quando una funzionalità è comune a entrambi. Utilizzato prevalentemente nella sezione dei requisiti RF_CLI.
•	CLR (Cliente Registrato) Attore che possiede un account sulla piattaforma. Rispetto al CLG dispone di funzionalità avanzate: homepage personale, storico prenotazioni, filtri per stato, annullamento con motivazione, riprenotazione rapida, modifica profilo e notifiche in-app.
•	Configurazione del Salone Insieme delle operazioni che il BAR esegue per definire le caratteristiche operative del salone: gestione delle poltrone, del catalogo servizi e delle fasce orarie di apertura e pausa. È il prerequisito affinché il sistema possa calcolare la disponibilità degli slot.
•	Credenziali Coppia e-mail/password utilizzata da BAR e CLR per autenticarsi nel sistema.
•	Dashboard Giornaliera Vista di dettaglio dell'agenda del giorno corrente, riservata al BAR. Mostra gli appuntamenti in ordine cronologico per poltrona e consente la gestione diretta delle prenotazioni.
•	Dashboard Settimanale Vista d'insieme dell'agenda della settimana corrente, riservata al BAR. Consente la pianificazione a medio termine degli appuntamenti suddivisi per poltrona e per giorno.
•	Double-booking Situazione in cui due prenotazioni occupano lo stesso slot orario sulla stessa poltrona. BarberBook previene questa condizione in modo automatico e centralizzato tramite il PrenotazioneController, che verifica sempre l'assenza di sovrapposizioni prima di creare o modificare qualsiasi prenotazione.
•	Fascia Oraria Intervallo temporale configurato dal BAR che definisce la disponibilità di una poltrona in un determinato giorno. Può essere di tipo apertura (slot prenotabili) o pausa (slot bloccati). Le fasce orarie sono la base su cui il sistema calcola gli slot disponibili.
•	Flusso di Prenotazione Sequenza di passi che un cliente (CLR o CLG) compie per inviare una richiesta di prenotazione: selezione del servizio, scelta del giorno, visualizzazione degli slot liberi, selezione di poltrona e orario, conferma. Per il CLG include la compilazione del form dati ospite.
•	Gestionale Termine informale con cui ci si riferisce all'insieme delle funzionalità riservate al BAR: dashboard, gestione poltrone, servizi, orari e prenotazioni. Rappresenta il cuore operativo di BarberBook dal punto di vista del barbiere.
•	Homepage Personale Schermata principale visualizzata dal CLR dopo il login. Mostra i prossimi appuntamenti confermati e le informazioni principali del salone.
•	Notifica Messaggio generato automaticamente dal sistema a seguito di un evento rilevante (nuova richiesta, conferma, rifiuto, cancellazione, annullamento). Le notifiche in-app sono destinate a BAR e CLR. Le notifiche verso CLG avvengono tramite canali esterni al sistema (telefono o SMS) e non sono modellate internamente.
•	Poltrona Risorsa fisica del salone su cui viene erogato il servizio. È l'unità base di organizzazione dell'agenda. Ogni prenotazione è associata a una specifica poltrona. Il BAR può aggiungere, rinominare e rimuovere poltrone in qualsiasi momento.
•	Prenotazione Entità centrale del sistema. Rappresenta l'occupazione di una poltrona per un determinato servizio in una specifica fascia oraria. Può trovarsi in uno dei seguenti stati: In Attesa, Confermata, Rifiutata, Annullata, Passata.
•	Prenotazione Diretta Prenotazione creata manualmente dal BAR tramite il gestionale, senza passare dal flusso cliente. Viene creata direttamente con stato Confermata, senza transitare per lo stato In Attesa.
•	Registrazione Veloce Funzionalità opzionale proposta al CLG al termine del flusso di prenotazione. Consente di creare un account CLR utilizzando i dati anagrafici già inseriti nel form ospite, senza doverli reinserire.
•	Richiesta di Prenotazione Prenotazione inviata da un cliente (CLR o CLG) che si trova nello stato In Attesa. Non è ancora un appuntamento confermato: diventa tale solo dopo l'esplicita accettazione da parte del BAR.
•	Riprenotazione Rapida Funzionalità riservata al CLR che consente di avviare un nuovo flusso di prenotazione pre-compilato con il servizio e la poltrona di un appuntamento passato, richiedendo al cliente la sola selezione di giorno e orario.
•	Responsive Design Caratteristica dell'interfaccia utente che garantisce la piena fruibilità del sistema su dispositivi con dimensioni dello schermo diverse (smartphone, tablet, PC) senza degradazione funzionale.
•	Salone L'entità fisica gestita dal sistema: il salone di barbiere di Tony. È caratterizzato dalle sue poltrone, dal catalogo servizi e dagli orari di apertura configurati dal BAR.
•	Servizio Prestazione offerta dal salone ai clienti (es. taglio capelli, barba, trattamenti). Ogni servizio ha un nome, una durata stimata in minuti e un prezzo. La durata del servizio è utilizzata dal sistema per calcolare correttamente gli slot disponibili ed evitare sovrapposizioni.
•	Slot Fascia oraria disponibile per la prenotazione su una specifica poltrona in un dato giorno. Uno slot è considerato libero se rientra negli orari di apertura configurati, non coincide con una pausa e non è già occupato da una prenotazione esistente.
•	Stato della Prenotazione Valore che descrive la fase del ciclo di vita in cui si trova una prenotazione. I valori possibili sono: In Attesa (richiesta inviata, in attesa di decisione del BAR), Confermata (accettata dal BAR o creata direttamente dal gestionale), Rifiutata (rifiutata dal BAR), Annullata (annullata dal CLR o cancellata dal BAR), Passata (appuntamento il cui orario è già trascorso).
•	Storico Prenotazioni Sezione riservata al CLR che raccoglie l'elenco completo delle proprie prenotazioni in ordine cronologico decrescente. Supporta il filtraggio per stato e consente di avviare le operazioni di annullamento e riprenotazione rapida.
•	Utente Autenticato Qualsiasi soggetto che ha effettuato il login con credenziali valide. Nel sistema gli utenti autenticabili sono BAR e CLR.
•	Vetrina Servizi Sezione pubblica del sistema, accessibile senza autenticazione, che mostra il catalogo dei servizi offerti dal salone con nome, durata e prezzo. È il punto di ingresso al flusso di prenotazione per tutti i clienti.

