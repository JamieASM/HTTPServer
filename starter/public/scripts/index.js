// On website load, do the following
document.addEventListener("DOMContentLoaded", function () {
    // key: id, value: artist
    const queueMap = new Map();

    // html elements
    const ticketsSection = document.getElementById("ticket-section");
    const template = document.getElementsByClassName("ticket-information")[0];
    const queueSection = document.getElementsByClassName("queue-info")[0];
    const queueElement = document.getElementsByClassName("queue-element")[0];
    const queueElementClone = queueElement.cloneNode(true);

    // hide this element
    queueElement.style.display = "none";

    // handle the 'GET /tickets/ requests'
    fetch("/tickets", {
        method: "GET",
        headers: {"Accept": "application/json"}
    })
        .then((response) => {
            // handle the case that the response is an error.
            if (!response.ok) {
                // only a 500 error is possible for this request type
                if (response.status === 500) {
                    return response.text().then(text => {
                        throw new Error(text || "Server Error");
                    })
                }
                throw new Error(response.statusText);
            }

            // otherwise, the response was ok
            return response.json();
    })
        .then(concerts => {
            if (concerts === "{}") {
                addTicketError("No concerts available");
            } else {
                // add these to the web page
                concerts.forEach(concert => {
                    addConcert(concert);
                });
            }

            // now they are all loaded, hide the first one
            template.style.display = "none";
        })
        .catch(err => {
            addTicketError(err.message || "Failed to retrieve resources");
        });

    /**
     * Generates a p element to show the client the error.
     * @param message The error message.
     */
    function addTicketError(message) {
        const errorElement = document.createElement("p");
        errorElement.textContent = message;

        ticketsSection.appendChild(errorElement);
    }

    /**
     * Creates a HTML element to represent a concert instance.
     * @param json The JSON object that contains the concert data.
     */
    function addConcert(json) {
        const clone = template.cloneNode(true);

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

    /**
     * Reformats the data to be human-readable.
     * @param datetime The date as represented by a ISO 8601 UTC string
     * @returns {string} The reformatted date. Uses dd/mm/yyyy.
     */
    function makeHumanReadable(datetime) {
        let day = datetime.slice(8,10);
        let month = datetime.slice(5,7);
        let year = datetime.slice(0, 4);

        return day + "/" + month + "/" + year;
    }

    /**
     * Requests the server to queue a ticket purchase request.
     * @param artist The artist who's tickets the client wants to purchase.
     * @param numberOfTickets The number of tickets the client wants to purchase.
     */
    function queue(artist, numberOfTickets) {
        fetch(`/queue/${artist.replaceAll(" ", "-")}`, {
            method: "POST",
            headers: {
                "Accept": "application/json",
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ tickets: numberOfTickets })
        })
            .then(res => {
                // 500 or 400 errors
                if (!res.ok) {
                    if (res.status === 400 || res.status === 500 ) {
                        // alert the user
                        return res.text().then(text => {
                            throw new Error(res.text || `Server error: ${res.status}`);
                        })
                    }
                    throw new Error(res.statusText);
                }

                return res.json()
            })
            .then(data => {
                window.alert(`Your request has been successfully added to the queue.\nYour Queue ID is: ${data.id}`);
                queueMap.set(data.id, artist);
            })
            .catch(error => {
                window.alert("Error: ", error);
            });
    }

    /**
     * Requests the user for the number of tickets they want to purchase.
     * @returns {number|null} The number of tickets the client wants to purchase.
     */
    function requestNumberOfTickets() {
        let res

        // prompt the user until the response is valid
        do {
            res = prompt("Please enter the number of tickets to be purchased: ");

            if (res === null) {
                return null;
            }

        } while (!Number.isInteger(Number(res)))

        return Number(res)
    }

    // periodic call to the reload the queue
    let intervalID = setInterval(updateQueue, 2000);

    /**
     * Updates the html elements in the queue section
     */
    function updateQueue() {
        if (queueMap.size === 0) {
            return;
        }

        // grabs the data for each queue element
        const fetchData = Array.from(queueMap.entries()).map(([queueId, artist]) => {
            return fetch(`/queue/${queueId}`, {
                method: "GET",
                headers: { "Accept": "application/json" }
            })
                .then(res => {
                    if (!res.ok) {
                        if (res.status === 404) {
                            return res.text().then(text => ({
                                position: -2,
                                errorMessage: text || "Queue not found",
                            }));
                        }
                        else if (res.status === 500) {
                            return res.text().then(text => ({
                                position: -2,
                                errorMessage: text || "Server Error",
                            }));
                        }
                    }

                    return res.json();
                })
                .then(data => {
                    // If purchase completed, update ticket count
                    if (data.position === -1 ) {
                        if (queueMap.has(data.id)) {
                            updateTicketCount(artist, data.tickets);
                            queueMap.delete(data.id);
                        }
                        return null; // and display nothing
                    }
                    return data;
                })
                .catch(error => {
                    return {
                        position: -2,
                        errorMessage: error.message || "Failed to retrieve queue information"
                    };
                });
        });

        Promise.all(fetchData).then(res => {
            document.querySelectorAll('.queue-element').forEach(e => e.remove());

            // Sort errors and valid results
            const validResults = res.filter(data => data && data.position >= 0);
            const errors = res.filter(data => data && data.position === -2);

            validResults.sort((a, b) => a.position - b.position);

            // display errors:
            if (errors.length > 0) {
                errors.forEach(error => {
                    addQueueError(error.errorMessage || "Failed to retrieve resources");
                })
            }

            // Add sorted queue elements
            validResults.forEach(data => {
                addQueueElement(data);
            });

        })
    }

    function addQueueError(message) {
        const errorElement = document.createElement("p");
        errorElement.textContent = message;
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

    function addQueueElement(data) {
        const clone = queueElementClone.cloneNode(true);
        clone.style.display = "";

        clone.getElementsByClassName("artist")[0].textContent = queueMap.get(data.id);
        clone.getElementsByClassName("position")[0].textContent = data.position;

        queueSection.appendChild(clone);
    }
})

