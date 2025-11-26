// get the tickets on the website load
document.addEventListener("DOMContentLoaded", function () {
    const queueMap = new Map();

    const ticketsSection = document.getElementById("ticket-section");
    const template = document.getElementById("ticket-information");
    const queueSection = document.getElementsByClassName("queue-info")[0];
    const queueElement = document.getElementsByClassName("queue-element")[0];
    const queueElementClone = queueElement.cloneNode(true);

    queueElement.style.display = "none";

    fetch("/tickets", {
        method: "GET",
        headers: {"Accept": "application/json"}
    })
        .then((response) => {
            if (!response.ok) {
                if (response.status === 500) {
                    return JSON.stringify({});
                }
                throw new Error(response.statusText);
            }
            return response.json();
    })
        .then(concerts => {
            if (concerts === "{}") {
                addTicketError();
            } else {
                // add these to the web page
                concerts.forEach(concert => {
                    addConcert(concert);
                });
            }

            // now they are all loaded, hide the first one
            template.style.display = "none";
        })
        .catch(err => console.error(err));

    function addTicketError() {
        const errorElement = document.createElement("p");
        errorElement.textContent = "Failed to retrieve resources";

        ticketsSection.appendChild(errorElement);
    }

    function addConcert(json) {
        const clone = template.cloneNode(true);

        clone.removeAttribute("id");

        // add the text
        clone.getElementsByClassName("artist")[0].textContent = json.artist;
        clone.getElementsByClassName("venue")[0].textContent = json.venue;
        clone.getElementsByClassName("datetime")[0].textContent = makeHumanReadable(json.datetime);
        clone.getElementsByClassName("count")[0].textContent = json.count;

        // Add the concert to the button and an associated queue event listener
        const button = clone.getElementsByClassName("join-queue")[0];
        button.dataset.artist = json.artist;

        // event listener
        button.addEventListener("click", () => {
            let numberOfTickets = requestNumberOfTickets();
            if (numberOfTickets !== null) {
                queue(button.dataset.artist, numberOfTickets);
            }
        })

        ticketsSection.appendChild(clone);
    }

    // I will die on this hill, the format provided is not human readable
    function makeHumanReadable(datetime) {
        let day = datetime.slice(8,10);
        let month = datetime.slice(5,7);
        let year = datetime.slice(0, 4);

        return day + "/" + month + "/" + year;
    }

// TODO: when a ticket is purchased, the number of tickets available should go down
    function queue(artist, numberOfTickets) {
        fetch(`/queue/${artist.replaceAll(" ", "-")}`, {
            method: "POST",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ tickets: numberOfTickets })
        })
            .then(res => res.json())
            .then(data => {
                window.alert(`Your request has been successfully added to the queue.\nYour Queue ID is: ${data.id}`);
                queueMap.set(data.id, artist);
            })
            .catch(error => console.error(error));
    }

    function requestNumberOfTickets() {
        let res

        do {
            res = prompt("Please enter the number of tickets to be purchased: ");

            if (res === null) {
                return null;
            }

        } while (!Number.isInteger(Number(res)))

        return Number(res)
    }

    // periodic call to the queue
    let intervalID = setInterval(updateQueue, 2000);

    function updateQueue() {
        if (queueMap.size === 0) {
            return;
        }

        // remove all queue elements
        //

        const fetchData = Array.from(queueMap.entries()).map(([queueId, artist]) => {
            return fetch(`/queue/${queueId}`, {
                method: "GET",
                headers: { "Accept": "application/json" }
            })
                .then(res => {
                    if (!res.ok) {
                        if (res.status === 404) {
                            return {
                                position: -2,
                            };
                        }
                        else if (res.status === 500) {
                            return {
                                position: -2,
                            };
                        }
                    }

                    return res.json();
                })
                .then(data => {
                    // If purchase completed, update ticket count
                    if (data.position === -1) {
                        updateTicketCount(artist, data.tickets);
                        return null; // Don't display
                    }
                    return data;
                })
                .catch(error => {
                    console.error(`Error fetching queue ${queueId}:`, error);
                    return null;
                });
        });

        Promise.all(fetchData).then(res => {
            document.querySelectorAll('.queue-element').forEach(e => e.remove());

            // Filter out null values and sort by position
            const validResults = res.filter(data => data && data.position !== -2);
            validResults.sort((a, b) => a.position - b.position);

            if (validResults.length === 0) {
                // Handle errors separately
                const hasErrors = res.some(data => data && data.position === -2);
                if (hasErrors) {
                    addQueueError();
                }
            }

            // Add sorted queue elements
            validResults.forEach(data => {
                addQueueElement(data);
            });

        })
    }

    function addQueueError() {
        const errorElement = document.createElement("p");
        errorElement.textContent = `Failed to retrieve resources`;
        errorElement.className = "queue-element";

        queueSection.appendChild(errorElement);
    }

    function updateTicketCount(artist, numberOfTickets) {
        const ticketElements = document.querySelectorAll(".ticket-information");

        ticketElements.forEach(element => {
           if (element.querySelector(".artist").textContent === artist) {
               const count = element.querySelector(".count");
               const currentCount = parseInt(count.textContent);

               count.textContent = currentCount - numberOfTickets;
           }
        });
    }

    // TODO: These elements are not added in order. Fix that
    function addQueueElement(data) {
        const clone = queueElementClone.cloneNode(true);
        clone.style.display = "";

        clone.getElementsByClassName("artist")[0].textContent = queueMap.get(data.id);
        clone.getElementsByClassName("position")[0].textContent = data.position;

        queueSection.appendChild(clone);
    }
})

